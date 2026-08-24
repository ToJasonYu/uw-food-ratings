package com.waterloo.uwfoodratings;

public record AuthResponse(String token, Long userId, String username) {
}
