package com.medsimple.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AccessGuardTest {

    private final AccessGuard guard = new AccessGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UUID userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── requirePatientSelfOrStaff ──────────────────────────────────────────────

    @Test
    void patientAccessingOwnRecords_allowed() {
        UUID me = UUID.randomUUID();
        authenticate(me, "PATIENT");

        assertThatCode(() -> guard.requirePatientSelfOrStaff(me)).doesNotThrowAnyException();
    }

    @Test
    void patientAccessingAnotherPatient_denied() {
        authenticate(UUID.randomUUID(), "PATIENT");

        assertThatThrownBy(() -> guard.requirePatientSelfOrStaff(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void staffAccessingAnyPatient_allowed() {
        authenticate(UUID.randomUUID(), "PHYSICIAN");

        // A clinician's id never equals the patient's, yet access is allowed.
        assertThatCode(() -> guard.requirePatientSelfOrStaff(UUID.randomUUID())).doesNotThrowAnyException();
    }

    // ── requireStaff ───────────────────────────────────────────────────────────

    @Test
    void requireStaff_deniesPatients() {
        authenticate(UUID.randomUUID(), "PATIENT");

        assertThatThrownBy(guard::requireStaff).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireStaff_allowsClinicStaff() {
        authenticate(UUID.randomUUID(), "NURSE");

        assertThatCode(guard::requireStaff).doesNotThrowAnyException();
    }

    // ── unauthenticated ────────────────────────────────────────────────────────

    @Test
    void noAuthentication_denied() {
        // Empty context — nothing authenticated.
        assertThatThrownBy(() -> guard.requirePatientSelfOrStaff(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
