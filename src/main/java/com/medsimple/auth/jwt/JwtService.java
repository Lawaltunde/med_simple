package com.medsimple.auth.jwt;

import com.medsimple.shared.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates stateless access-token JWTs.
 * Claims: subject = userId, plus email and role. Signed HS256 with jwt.secret.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-seconds:900}") long accessTtlSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMillis = accessTtlSeconds * 1000;
    }

    public String issueAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtlMillis))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlMillis / 1000;
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }
}
