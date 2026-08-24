# Auth design notes

This document explains the authentication rewrite: what was broken, what replaced it,
and why each piece works the way it does. Written to be the reference for explaining
this work out loud (e.g. in an interview), not just a changelog.

## What was broken

Three separate problems, in `AuthController` and `RatingController`:

1. **Plaintext passwords.** `login()` compared passwords with
   `user.get().getPassword().equals(loginRequest.getPassword())`. That only works if
   the password is stored exactly as the user typed it — meaning `register()` saved
   whatever string the client sent, with no transformation at all. Anyone who could
   read the `app_user` table (a DB backup, a misconfigured export, an unrelated SQL
   injection bug, a curious contractor) would see every user's real password in
   cleartext. Because people reuse passwords across sites, that's not just "this app's
   problem" — it's a credential leak for every other account that user reused it on.

2. **No proof of identity after login.** `login()` returned the raw `User` object and
   nothing else. There was no session cookie, no token — nothing that a later request
   could present as evidence "this request really came from user 7." Every
   "authenticated" action (`createRating`, `upvoteRating`, `deleteRating`) instead took
   a `userId` as a plain request parameter or request body field and trusted it
   outright. Any client could open dev tools, change `userId=7` to `userId=1`, and
   act as a different user with zero credentials.

3. **A hardcoded bypass.** `deleteRating()` had `if (userId == 1L || ...)` — meaning
   user ID 1 could delete *any* rating, no ownership check at all. Combined with #2
   (client-supplied `userId`), this wasn't even "user 1 has admin powers" — it was
   "anyone who sends `userId=1` has admin powers," since nothing verified the caller
   actually was user 1.

These three compound: #2 is what makes #3 exploitable by anyone, not just the real
user 1.

## Fix 1: BCrypt password hashing

`register()` now calls `passwordEncoder.encode(rawPassword)` before saving; `login()`
calls `passwordEncoder.matches(rawPassword, storedHash)` instead of `.equals()`.

**Why BCrypt and not something like SHA-256:**

- **Per-password salt.** BCrypt generates a random salt and stores it *inside* the
  output hash string itself (e.g. `$2a$10$N9qo8uLOickgx2ZMRZoMye...`). Two users with
  the password `"password123"` get completely different hashes. A generic hash like
  SHA-256 is deterministic — same input, same output — which means an attacker can
  precompute a "rainbow table" of hash → common password and reverse every match in
  the database in one pass. Salting defeats that: the attacker would need a separate
  rainbow table per salt, which is the same cost as brute-forcing each password
  individually.
- **Deliberate slowness.** BCrypt has a tunable work factor (the `$10$` in the hash
  above — 2^10 rounds by default in Spring's `BCryptPasswordEncoder`). SHA-256 is
  *fast* by design, which is exactly wrong for password storage: a GPU can compute
  billions of SHA-256 hashes per second, so a leaked SHA-256 hash database can be
  brute-forced quickly. BCrypt is intentionally slow enough that hashing one password
  takes milliseconds (fine for a single login) but brute-forcing millions of guesses
  becomes computationally expensive. The work factor can be raised over time as
  hardware gets faster, without changing the algorithm.

In short: SHA-256 is designed to be fast and reproducible (good for checksums,
wrong for passwords); BCrypt is designed to be slow and unique-per-input (right for
passwords, wrong for checksums). Storing a password hash is not the same problem as
verifying file integrity, and using a fast general-purpose hash for it is the classic
mistake BCrypt exists to prevent.

## Fix 2: JWT-based auth

On successful login, the server now returns a **JWT** (JSON Web Token) instead of the
raw user. The frontend stores it and attaches it as `Authorization: Bearer <token>` on
every request to a protected endpoint. A `JwtAuthFilter` (a servlet filter that runs
before the request reaches any controller) validates the token and extracts the
authenticated user's ID.

**What a JWT actually is:**

A JWT is a string with three base64url-encoded parts separated by dots:
`header.payload.signature`.

- **Header** — metadata, e.g. which signing algorithm was used (`HS256` here).
- **Payload** — the claims: this app puts the user's ID in the `sub` (subject) claim,
  plus `iat` (issued-at) and `exp` (expiry) timestamps.
- **Signature** — an HMAC computed over the header and payload, using a secret key
  only the server knows (`app.jwt.secret`).

Crucially, the header and payload are only *encoded*, not encrypted — anyone can
base64-decode a JWT and read the claims (don't put secrets in the payload). What makes
it trustworthy is the signature: `JwtService.generateToken()` signs the token with the
server's secret key when it's issued, and `validateAndGetUserId()` recomputes that
signature from the token's own header+payload and checks it matches. If a client edits
the payload (e.g. changes `sub` from `7` to `1`), the signature no longer matches what
the server recomputes, and `Jwts.parser()...parseSignedClaims(token)` throws — the
`JwtAuthFilter` catches that and returns 401. The client cannot produce a valid
signature for a forged payload without knowing the secret key, which never leaves the
server.

**Stateless auth vs. server-side sessions:**

A traditional session-based login stores session state (which user is logged in) in
server memory or a session store (Redis, a DB table), keyed by a session ID the client
holds in a cookie. Every request means a lookup: "does this session ID exist, and who
does it belong to?"

A JWT carries its own proof — the userId and expiry are *in* the token, and the
signature proves the server issued it. Validating a request means checking a signature
locally; no DB or session-store lookup needed. That's what "stateless" means here: the
server doesn't need to remember anything about active sessions.

Trade-offs:
- **Session-based:** trivial to revoke (delete the session server-side) and to inspect
  active sessions, but requires a shared session store if you have multiple server
  instances, and a lookup on every request.
- **JWT:** scales horizontally with no shared state and no per-request DB hit, but
  revoking a single token before its expiry is hard (the server has no record of it —
  you'd need a denylist, which reintroduces state). This app mitigates that with a
  short-ish expiry (`app.jwt.expiration-ms`, currently 24h) rather than building a
  denylist, which is the standard trade-off for a project this size.

## Fix 3: removing the `userId == 1L` bypass

**Why the old code was vulnerable:** `deleteRating()` trusted a `userId` request
parameter supplied by the client with no verification. The `userId == 1L` clause was
very likely a dev shortcut — "user 1 is me, in my local test data, let me always be
able to clean up test rows" — that never got removed. In a real deployment, it's not
"user 1 has extra powers," it's "*anyone* who appends `?userId=1` to the DELETE
request has extra powers," because nothing on the server confirmed the caller's
identity. That's a backdoor: a magic value that skips the authorization check
entirely, reachable by anyone who reads the API or the frontend source.

**Old flow:**
```
DELETE /api/ratings/42?userId=1
  → server reads userId=1 straight from the query string
  → userId == 1L is true → delete succeeds, no questions asked
```
Nothing here proves the request came from an authenticated user at all.

**New flow:**
```
DELETE /api/ratings/42
  Authorization: Bearer <jwt>
  → JwtAuthFilter validates the signature, extracts userId from the token's
    verified claims, stores it as a request attribute
  → RatingController reads userId from that attribute (not from any client input)
  → compares it against rating.getUserId() → 403 if they don't match
```

The fix isn't just "delete the `== 1L` line" — it's that `userId` is no longer a piece
of data the client gets to assert. It's derived entirely from a token the server
itself signed, so a client can influence *which* userId that is only by logging in as
that user in the first place.

## Migration note: existing plaintext passwords

Any accounts created before this change have plaintext passwords stored in
`app_user.password`. `passwordEncoder.matches(raw, stored)` will not match a plaintext
value against a BCrypt hash, so those accounts cannot log in as-is.

Chosen approach: **no migration — re-register.** This is a local dev/portfolio
project with no real user base, so the simplest correct option is to drop/recreate
the `app_user` table (or just re-register test accounts) rather than write a one-time
"detect plaintext, hash on next login" migration. The trade-off: a re-hash-on-login
migration is the right call for a real production system with real users who can't be
asked to re-register, but it adds a permanent code path (detecting "is this a legacy
plaintext value") that has no purpose once every account has migrated. For a project
at this stage, that complexity isn't worth it — it exists only to describe the
trade-off for whoever reads this next (including future-me, in an interview).

## Config notes

`app.jwt.secret` defaults to a placeholder dev value in `application.properties`
(`dev-only-signing-key-...`) — anyone can read it in this public repo, which is fine
for local development but **must** be overridden via the `JWT_SECRET` environment
variable in any real deployment. If the signing key is public, anyone can mint their
own validly-signed tokens for any userId, which defeats the entire scheme.
