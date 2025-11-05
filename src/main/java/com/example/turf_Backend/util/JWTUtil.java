package com.example.turf_Backend.util;

import com.example.turf_Backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt.secretKey}")
    private String secretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }


    public String generateToken(User user) { // Generate Token using new builder style
        return Jwts.builder()
                .subject(user.getEmail()) // new style
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(getSecretKey())
                .compact();
    }

    //  Extract Email (Subject) from Token
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey()) // replaces setSigningKey()
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }


    public Claims getAllClaims(String token) {    //  Extract Any Claim
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    private boolean isTokenExpired(String token) {// Check Token Expiration
        Date expiration = getAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }


    public boolean isTokenValid(String token, User user) {//  Validate Token (email matches + not expired)
        String email = getEmailFromToken(token);
        return (email.equals(user.getEmail()) && !isTokenExpired(token));
    }
}
