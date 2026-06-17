package com.medsimple.auth.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ownership checks for patient-scoped resources, enforced in the service layer (the same
 * place the PHIPA result-release gate lives). Role checks alone are not enough here: a
 * PATIENT must reach only their own data, while clinic staff may reach any patient.
 *
 * Reads the authenticated principal (userId) and role from the SecurityContext, so it only
 * works on request threads. Every method that calls it is reachable solely over HTTP.
 */
@Component
public class AccessGuard {

    private static final String PATIENT_AUTHORITY = "ROLE_PATIENT";

    /** The authenticated user's id. */
    public UUID currentUserId() {
        return (UUID) authentication().getPrincipal();
    }

    /** True when the caller holds the PATIENT role (vs. clinic staff). */
    public boolean isPatient() {
        return authentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PATIENT_AUTHORITY::equals);
    }

    /**
     * Patient-scoped access: a PATIENT may act only on their own {@code patientId}; any
     * non-patient (clinic staff) is allowed through. Throws {@link AccessDeniedException}
     * (mapped to 403) otherwise.
     */
    public void requirePatientSelfOrStaff(UUID patientId) {
        if (isPatient() && !currentUserId().equals(patientId)) {
            throw new AccessDeniedException("You can only access your own records");
        }
    }

    /** Staff-only action: deny patients outright (e.g. AI processing callbacks). */
    public void requireStaff() {
        if (isPatient()) {
            throw new AccessDeniedException("This action is restricted to clinic staff");
        }
    }

    private Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return auth;
    }
}
