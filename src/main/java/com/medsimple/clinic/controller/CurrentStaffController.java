package com.medsimple.clinic.controller;

import com.medsimple.clinic.dto.ClinicResponse;
import com.medsimple.clinic.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Self-scoped clinic lookups for the authenticated user. Lets the clinician console
 * discover which clinic's queue to load without the client knowing a clinic id up front.
 * Patients get an empty list (they are not staff anywhere).
 */
@RestController
@RequiredArgsConstructor
public class CurrentStaffController {

    private final ClinicService clinicService;

    @GetMapping("/api/v1/me/clinics")
    public ResponseEntity<List<ClinicResponse>> myClinics(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(clinicService.getClinicsForUser(userId));
    }
}
