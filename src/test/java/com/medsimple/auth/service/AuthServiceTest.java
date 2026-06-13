package com.medsimple.auth.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.dto.AuthResponse;
import com.medsimple.auth.dto.LoginRequest;
import com.medsimple.auth.entity.RefreshToken;
import com.medsimple.auth.exception.InvalidCredentialsException;
import com.medsimple.auth.exception.InvalidTokenException;
import com.medsimple.auth.jwt.JwtService;
import com.medsimple.auth.repository.RefreshTokenRepository;
import com.medsimple.auth.repository.UserAccountRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserAccountRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;

    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTtlSeconds", 2592000L);
        user = User.builder()
                .id(UUID.randomUUID()).email("jane@example.com")
                .passwordHash("hashed").role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").active(true).build();
    }

    // ── login ───────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_issuesTokens() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.issueAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("jane@example.com", "password123"), "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.userId()).isEqualTo(user.getId());
        verify(auditService).log(user.getId(), "LOGIN", "users", user.getId(), "127.0.0.1");
        // The stored refresh token is hashed, never the raw value.
        verify(refreshTokenRepository).save(argThat(t -> !t.getTokenHash().equals(response.refreshToken())));
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong"), null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "x"), null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_inactiveUser_throws() {
        user.setActive(false);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "password123"), null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── refresh (rotation) ──────────────────────────────────────────────────────

    @Test
    void refresh_activeToken_rotatesAndIssuesNew() {
        // Build a stored token whose hash matches a known raw value.
        String rawToken = "raw-refresh-value";
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusDays(1)).revoked(false).build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));
        when(jwtService.issueAccessToken(user)).thenReturn("new-access");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refresh(rawToken);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue(); // old token rotated out
        // saved twice: once to revoke old, once for the new token
        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void refresh_unknownToken_throws() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("nope"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_revokedToken_throws() {
        String rawToken = "revoked-raw";
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusDays(1)).revoked(true).build();
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── logout ──────────────────────────────────────────────────────────────────

    @Test
    void logout_revokesToken() {
        String rawToken = "logout-raw";
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusDays(1)).revoked(false).build();
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        authService.logout(rawToken);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }
}
