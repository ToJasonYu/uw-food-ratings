package com.waterloo.uwfoodratings;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingRepository ratingRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    public RatingController(RatingRepository ratingRepository, VoteRepository voteRepository, UserRepository userRepository) {
        this.ratingRepository = ratingRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    private Long authenticatedUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtAuthFilter.AUTH_USER_ID_ATTRIBUTE);
    }

    @GetMapping
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll();
    }

    @PostMapping
    public Rating createRating(@RequestBody Rating rating, HttpServletRequest request) {
        rating.setUserId(authenticatedUserId(request));
        rating.setTimestamp(LocalDateTime.now());
        rating.setUpvotes(0);
        return ratingRepository.save(rating);
    }

    @PutMapping("/{id}/upvote")
    public Rating upvoteRating(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authenticatedUserId(request);

        if (voteRepository.existsByUserIdAndRatingId(userId, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already voted for this!");
        }

        Vote vote = new Vote(null, userId, id);
        voteRepository.save(vote);

        return ratingRepository.findById(id).map(rating -> {
            rating.setUpvotes(rating.getUpvotes() + 1);
            return ratingRepository.save(rating);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteRating(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authenticatedUserId(request);
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found"));

        boolean isOwner = rating.getUserId() != null && rating.getUserId().equals(userId);
        boolean isAdmin = userRepository.findById(userId).map(User::isAdmin).orElse(false);

        if (isOwner || isAdmin) {
            ratingRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own posts.");
        }
    }
}