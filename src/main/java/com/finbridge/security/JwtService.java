package com.finbridge.security;

import com.finbridge.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("name", user.getName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** B2B token carries orgUserId as subject + organizationId claim */
    public String generateB2BToken(String orgUserId, String organizationId) {
        return Jwts.builder()
                .subject(orgUserId)
                .claim("organizationId", organizationId)
                .claim("type", "b2b")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    /** Short-lived, purpose-scoped token used for email verification / password reset links. */
    public String generatePurposeToken(String userId, String purpose, long ttlMs) {
        return Jwts.builder()
                .subject(userId)
                .claim("purpose", purpose)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key())
                .compact();
    }

    /** Returns the subject (userId) if the token is valid and matches the expected purpose, else null. */
    public String verifyPurposeToken(String token, String purpose) {
        try {
            Claims claims = extractClaims(token);
            if (!purpose.equals(claims.get("purpose"))) return null;
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractOrganizationId(String token) {
        return (String) extractClaims(token).get("organizationId");
    }

    public boolean isB2BToken(String token) {
        return "b2b".equals(extractClaims(token).get("type"));
    }
}
