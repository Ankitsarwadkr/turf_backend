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

        String path = request.getRequestURI();

        if (
        if (
                path.equals("/api/auth/login") ||
                        path.equals("/api/auth/register/customer") ||
                        path.equals("/api/auth/register/owner") ||
                        path.equals("/api/auth/forget-password") ||
                        path.equals("/api/auth/reset-password") ||
                        path.startsWith("/api/public/") ||
                        path.startsWith("/health")
        ) {
            filterChain.doFilter(request, response);
            return;
        }
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String email = null;

        // Try to read JWT from cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
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
        if (email !=null && SecurityContextHolder.getContext().getAuthentication()==null)
        {
            try
            {
                //load authorities
                UserDetails userDetails=customUserDetailsService.loadUserByUsername(email);

                //Ensure we have the application User entity as principle
                User userPrinciple;
                if (userDetails instanceof User)
                {
                    //your CustomUserDetails Service already returned the entity
                    userPrinciple=(User) userDetails;
                }
                else
                {
                    //fallback load application User from Db (add method in CustomUserDetailsService)
                    userPrinciple=customUserDetailsService.loadUserEntityByEmail(email);
                }
                if (jwtUtil.isTokenValid(token,userPrinciple))
                {
                 UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(userPrinciple,null,userDetails.getAuthorities());
                 authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                 SecurityContextHolder.getContext().setAuthentication(authToken);

                 log.info("[{}] User '{}' accessed '{} {}' at {}",userDetails.getAuthorities(),userDetails.getUsername(),request.getMethod(),request.getRequestURI(),LocalDateTime.now());
                }
                else
                {
                    log.warn("invalid Jwt for user : {}",email);
                }
            }
            catch (Exception ex)
            {
                log.error("Authentication setup failed for token user '{}' : {}",email,ex.getMessage());
            }
        } else if (email==null) {
            log.debug("No JWT Present for request : {}",request.getRequestURI());
        }

        //Continue filter chain
        filterChain.doFilter(request, response);
    }
}
