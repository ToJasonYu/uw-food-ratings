package com.waterloo.uwfoodratings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityBeansConfig.class, JwtService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void registerThenLoginRoundTrip() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        String registerBody = objectMapper.writeValueAsString(new AuthRequest("alice", "correct horse battery staple"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value(org.hamcrest.Matchers.not("correct horse battery staple")));

        User storedUser = new User(1L, "alice", passwordEncoder.encode("correct horse battery staple"), false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(storedUser));

        String loginBody = objectMapper.writeValueAsString(new AuthRequest("alice", "correct horse battery staple"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        User storedUser = new User(1L, "alice", passwordEncoder.encode("correct horse battery staple"), false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(storedUser));

        String loginBody = objectMapper.writeValueAsString(new AuthRequest("alice", "wrong password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }
}
