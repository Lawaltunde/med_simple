package com.medsimple.auth.jwt;

import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-signing-secret-0123456789abcdefghijklmnop";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900);
        user = User.builder()
                .id(UUID.randomUUID())
                .email("doc@example.com")
                .role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("Smith")
                .build();
    }

    @Test
    void issueAndParse_roundTripsClaims() {
        String token = jwtService.issueAccessToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractRole(token)).isEqualTo("PHYSICIAN");
        assertThat(jwtService.parse(token).get("email", String.class)).isEqualTo("doc@example.com");
    }

    @Test
    void parse_tamperedToken_throws() {
        String token = jwtService.issueAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void parse_tokenSignedWithDifferentSecret_throws() {
        JwtService other = new JwtService("a-totally-different-secret-key-9876543210zyxwvutsrqponml", 900);
        String foreignToken = other.issueAccessToken(user);

        assertThatThrownBy(() -> jwtService.parse(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_throws() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 0); // expires immediately
        String token = shortLived.issueAccessToken(user);
        Thread.sleep(1000);

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void accessTokenTtl_isExposed() {
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }
}
