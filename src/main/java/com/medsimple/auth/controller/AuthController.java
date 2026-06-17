package com.medsimple.auth.controller;

import com.medsimple.auth.dto.AuthResponse;
import com.medsimple.auth.dto.AuthTokens;
import com.medsimple.auth.dto.CurrentUserResponse;
import com.medsimple.auth.dto.LoginRequest;
import com.medsimple.auth.exception.InvalidTokenException;
import com.medsimple.auth.security.RefreshCookie;
import com.medsimple.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookie refreshCookie;

    @PostMapping("/login")
    public ResponseEntity<AuthTokens> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        AuthResponse tokens = authService.login(request, httpRequest.getRemoteAddr());
        return withRefreshCookie(tokens);
    }

    // Refresh token is read from the httpOnly cookie, never the request body.
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokens> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("No refresh token present");
        }
        AuthResponse tokens = authService.refresh(refreshToken);
        return withRefreshCookie(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        // Always clear the cookie, even if it was already gone.
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString())
                .build();
    }

    // Requires a valid access token; principal is the authenticated userId.
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    // Sets the rotated refresh token as an httpOnly cookie and returns only the access token.
    private ResponseEntity<AuthTokens> withRefreshCookie(AuthResponse tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(tokens.refreshToken()).toString())
                .body(AuthTokens.from(tokens));
    }
}
