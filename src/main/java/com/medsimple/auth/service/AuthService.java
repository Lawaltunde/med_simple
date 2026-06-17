package com.medsimple.auth.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.dto.AuthResponse;
import com.medsimple.auth.dto.CurrentUserResponse;
import com.medsimple.auth.dto.LoginRequest;
import com.medsimple.auth.entity.RefreshToken;
import com.medsimple.auth.exception.InvalidCredentialsException;
import com.medsimple.auth.exception.InvalidTokenException;
import com.medsimple.auth.jwt.JwtService;
import com.medsimple.auth.repository.RefreshTokenRepository;
import com.medsimple.auth.repository.UserAccountRepository;
import com.medsimple.auth.security.LoginRateLimiter;
import com.medsimple.shared.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final LoginRateLimiter rateLimiter;

    @Value("${jwt.refresh-token-ttl-seconds:2592000}") // 30 days
    private long refreshTtlSeconds;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        // Lockout check first — a locked identifier never reaches credential validation.
        rateLimiter.assertAllowed(request.email());

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !user.isActive()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException();
        }

        rateLimiter.recordSuccess(request.email());
        auditService.log(user.getId(), "LOGIN", "users", user.getId(), ipAddress);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = TokenHasher.sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not recognized"));

        // Reuse detection: a presented token that was already rotated out (revoked) means
        // either a stolen token is being replayed or the legitimate client raced. Either way
        // it is a compromise signal — revoke every session for the user and force re-login.
        if (stored.isRevoked()) {
            UUID userId = stored.getUser().getId();
            refreshTokenRepository.revokeAllForUser(userId);
            auditService.log(userId, "REFRESH_TOKEN_REUSE", "refresh_tokens", stored.getId(), null);
            throw new InvalidTokenException("Refresh token reuse detected; all sessions revoked");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Refresh token is expired");
        }

        // Rotation: revoke the presented token, issue a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = TokenHasher.sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));
        return new CurrentUserResponse(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRole());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user);

        String rawRefresh = TokenHasher.generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(rawRefresh))
                .expiresAt(OffsetDateTime.now().plusSeconds(refreshTtlSeconds))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken, rawRefresh, "Bearer",
                jwtService.getAccessTokenTtlSeconds(), user.getId(), user.getRole());
    }
}
