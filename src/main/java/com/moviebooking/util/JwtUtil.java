package com.moviebooking.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Secret and expiration are read from application.properties.
    // WHY not hardcode them here? Environment-specific values (especially secrets)
    // should never be in source code. Externalizing them lets different
    // environments (dev, prod) use different values without code changes.
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // milliseconds (86400000 = 24 hours)

    // WHY HMAC-SHA256?
    // JWT can be signed with asymmetric (RS256) or symmetric (HS256) algorithms.
    // HS256 uses a single shared secret — simpler and sufficient for a backend-only
    // API where only our server signs and verifies tokens. RS256 would be needed
    // if external services also need to verify tokens without the secret.
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a signed JWT containing the user's email and ID.
     * WHY embed userId in the token?
     * Every authenticated request needs the user's ID to scope DB queries
     * (e.g., "get MY bookings"). Embedding it avoids a DB lookup on every request.
     */
    public String generateToken(Long userId, String email) {
        return Jwts.builder()
                .setSubject(email)           // standard "sub" claim — who the token belongs to
                .claim("userId", userId)     // custom claim — used by controllers
                .setIssuedAt(new Date())     // "iat" — when the token was issued
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // "exp" — when it expires
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // sign to prevent tampering
                .compact();
    }

    /**
     * Validates the token signature and expiry, then returns its claims.
     * WHY validate signature? The JWT payload is Base64-encoded (not encrypted),
     * so anyone can decode it. The signature proves it was issued by our server
     * and hasn't been tampered with. If a user modifies their userId in the token,
     * the signature check here will fail.
     */
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token) // throws JwtException if invalid/expired
                .getBody();
    }

    public String extractEmail(String token) {
        return validateToken(token).getSubject();
    }

    public Long extractUserId(String token) {
        return validateToken(token).get("userId", Long.class);
    }

    /**
     * Safe boolean check — used by JwtAuthFilter to avoid try/catch noise there.
     * Returns false for expired tokens, tampered tokens, or malformed tokens.
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException covers: expired, wrong signature, malformed
            // IllegalArgumentException covers: null or empty token string
            return false;
        }
    }
}
