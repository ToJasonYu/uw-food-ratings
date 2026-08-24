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

        String attackerToken = jwtService.generateToken(attackerId);

        mockMvc.perform(delete("/api/ratings/10")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }
}
