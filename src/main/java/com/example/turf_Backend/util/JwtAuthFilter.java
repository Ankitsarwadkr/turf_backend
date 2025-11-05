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
import org.springframework.security.core.userdetails.UserDetailsService;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

String token=null;
String email =null;

if(request.getCookies()!=null)
{
    for (Cookie cookie: request.getCookies())
    {
        if(cookie.getName().equals("token"))
        {
            token=cookie.getValue();
        }
    }
}

            log.info("Incomming Request :{}",request.getRequestURI());
            final String authHeader =request.getHeader("Authorization");
            if (authHeader!=null && authHeader.startsWith("Bearer ")) // Checks the incoming request has Authorization Header
            {
                 token=authHeader.substring(7);
                try
                {
                     email= jwtUtil.getEmailFromToken(token);
                }
                catch (Exception e)
                {
                    log.error("Jwt parsing error : {}",e.getMessage());
                }
            }
            if (email!=null && SecurityContextHolder.getContext().getAuthentication()==null)
            {
                UserDetails userDetails=customUserDetailsService.loadUserByUsername(email);
                if (jwtUtil.isTokenValid(token,(User) userDetails))
                {
                    UsernamePasswordAuthenticationToken authoken=new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                    authoken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authoken);

                    log.info("{[]} User '{}' accessed '{} {}' at {} ",userDetails.getAuthorities()
                    ,userDetails.getUsername()
                    ,request.getMethod()
                    ,request.getRequestURI()
                    , LocalDateTime.now());
                }
                else {
                    log.warn("Invalid Jwt for User :{}",email);
                }
            }
            else if (email==null)
            {
                log.debug("No Jwt token found for request: {}",request.getRequestURI());
            }
            filterChain.doFilter(request,response);
    }
}
