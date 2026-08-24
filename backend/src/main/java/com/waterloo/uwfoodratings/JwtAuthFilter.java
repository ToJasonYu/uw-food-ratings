package com.waterloo.uwfoodratings;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the JWT on protected endpoints and stores the authenticated userId
 * as a request attribute, so controllers never trust a client-supplied userId.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_USER_ID_ATTRIBUTE = "authUserId";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
            return;
        }

        String token = header.substring("Bearer ".length());
        try {
            Long userId = jwtService.validateAndGetUserId(token);
            request.setAttribute(AUTH_USER_ID_ATTRIBUTE, userId);
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/") || path.startsWith("/actuator") || path.startsWith("/api/auth/")) {
            return true;
        }
        return "GET".equals(method) && path.equals("/api/ratings");
    }
}
