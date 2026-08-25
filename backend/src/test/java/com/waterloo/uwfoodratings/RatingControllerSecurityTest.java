package com.waterloo.uwfoodratings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
@Import(JwtService.class)
class RatingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RatingRepository ratingRepository;

    @MockitoBean
    private VoteRepository voteRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void corsPreflightOnProtectedEndpointIsNotRejectedForMissingToken() throws Exception {
        mockMvc.perform(options("/api/ratings")
                        .header("Origin", "https://uw-food-ratings-frontend.onrender.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401) {
                        throw new AssertionError("preflight OPTIONS request was rejected by the auth filter");
                    }
                });
    }

    @Test
    void createRatingWithNoTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/ratings")
                        .contentType("application/json")
                        .content("{\"restaurantName\":\"REV\",\"dishName\":\"Pho\",\"stars\":5,\"comment\":\"great\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRatingWithGarbageTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType("application/json")
                        .content("{\"restaurantName\":\"REV\",\"dishName\":\"Pho\",\"stars\":5,\"comment\":\"great\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingSomeoneElsesRatingIsForbidden() throws Exception {
        Long ownerId = 1L;
        Long attackerId = 2L;

        Rating existingRating = new Rating(10L, ownerId, "REV", "Pho", 5, "great", 0, LocalDateTime.now());
        when(ratingRepository.findById(10L)).thenReturn(Optional.of(existingRating));
        when(userRepository.findById(attackerId)).thenReturn(Optional.of(new User(attackerId, "attacker", "hash", false)));

        String attackerToken = jwtService.generateToken(attackerId);

        mockMvc.perform(delete("/api/ratings/10")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteSomeoneElsesRating() throws Exception {
        Long ownerId = 1L;
        Long adminId = 2L;

        Rating existingRating = new Rating(10L, ownerId, "REV", "Pho", 5, "great", 0, LocalDateTime.now());
        when(ratingRepository.findById(10L)).thenReturn(Optional.of(existingRating));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(new User(adminId, "admin", "hash", true)));

        String adminToken = jwtService.generateToken(adminId);

        mockMvc.perform(delete("/api/ratings/10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
