package com.medsimple.auth.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.dto.AuthResponse;
import com.medsimple.auth.dto.LoginRequest;
import com.medsimple.auth.entity.RefreshToken;
import com.medsimple.auth.exception.InvalidCredentialsException;
import com.medsimple.auth.exception.InvalidTokenException;
import com.medsimple.auth.exception.TooManyAttemptsException;
import com.medsimple.auth.jwt.JwtService;
import com.medsimple.auth.repository.RefreshTokenRepository;
import com.medsimple.auth.repository.UserAccountRepository;
import com.medsimple.auth.security.LoginRateLimiter;
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
    @Mock LoginRateLimiter rateLimiter;

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
        // A successful login clears the failed-attempt counter for that email.
        verify(rateLimiter).recordSuccess("jane@example.com");
        verify(rateLimiter, never()).recordFailure(anyString());
    }

    @Test
    void login_lockedOut_throwsBeforeCheckingCredentials() {
        doThrow(new TooManyAttemptsException("locked")).when(rateLimiter).assertAllowed("jane@example.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "password123"), null))
                .isInstanceOf(TooManyAttemptsException.class);

        // Credentials are never even looked up when the identifier is locked.
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void login_wrongPassword_recordsFailureAndThrows() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong"), null))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(rateLimiter).recordFailure("jane@example.com");
        verify(rateLimiter, never()).recordSuccess(anyString());
    }

    @Test
    void login_unknownEmail_recordsFailureAndThrows() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "x"), null))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(rateLimiter).recordFailure("ghost@example.com");
    }

    @Test
    void login_inactiveUser_recordsFailureAndThrows() {
        user.setActive(false);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "password123"), null))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(rateLimiter).recordFailure("jane@example.com");
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
    void refresh_reuseOfRevokedToken_revokesAllSessionsAndThrows() {
        // Replaying an already-rotated (revoked) token is a compromise signal.
        String rawToken = "revoked-raw";
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusDays(1)).revoked(true).build();
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidTokenException.class);

        // Whole token family for the user is revoked, and the reuse is audited.
        verify(refreshTokenRepository).revokeAllForUser(user.getId());
        verify(auditService).log(user.getId(), "REFRESH_TOKEN_REUSE", "refresh_tokens", revoked.getId(), null);
    }

    @Test
    void refresh_expiredToken_throwsWithoutFamilyRevoke() {
        String rawToken = "expired-raw";
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().minusMinutes(1)).revoked(false).build();
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidTokenException.class);
        // Plain expiry is not a compromise — sessions are left alone.
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
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
