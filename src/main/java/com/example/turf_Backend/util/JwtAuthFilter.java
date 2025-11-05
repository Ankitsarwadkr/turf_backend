package com.example.turf_Backend.util;

import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String email = null;

        // Try to read JWT from cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }


        log.info("Incoming request: {}", request.getRequestURI());

        //  Extract email from token if found
        if (token != null) {
            try {
                email = jwtUtil.getEmailFromToken(token);
            } catch (Exception e) {
                log.error("JWT parsing error: {}", e.getMessage());
            }
        }

        //  Authenticate user if valid
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            if (jwtUtil.isTokenValid(token, (User) userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("[{}] User '{}' accessed '{} {}' at {}",
                        userDetails.getAuthorities(),
                        userDetails.getUsername(),
                        request.getMethod(),
                        request.getRequestURI(),
                        LocalDateTime.now());
            } else {
                log.warn("Invalid JWT for user: {}", email);
            }
        } else if (email == null) {
            log.debug("No valid JWT found for request: {}", request.getRequestURI());
        }

        //Continue filter chain
        filterChain.doFilter(request, response);
    }
}
