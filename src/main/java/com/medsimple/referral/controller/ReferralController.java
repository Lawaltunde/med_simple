package com.medsimple.referral.controller;

import com.medsimple.referral.dto.*;
import com.medsimple.referral.service.ReferralService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Authoring, confirming the specialty, and submitting a referral are physician-only.
@RestController
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/referrals")
    public ResponseEntity<ReferralResponse> create(@Valid @RequestBody CreateReferralRequest request,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referralService.createReferral(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/referrals/{id}")
    public ResponseEntity<ReferralResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(referralService.getReferral(id));
    }

    @GetMapping("/api/v1/care-plans/{carePlanId}/referrals")
    public ResponseEntity<List<ReferralResponse>> getForCarePlan(@PathVariable UUID carePlanId) {
        return ResponseEntity.ok(referralService.getCarePlanReferrals(carePlanId));
    }

    @GetMapping("/api/v1/patients/{patientId}/referrals")
    public ResponseEntity<List<ReferralResponse>> getForPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(referralService.getPatientReferrals(patientId));
    }

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/referrals/{id}/confirm-specialty")
    public ResponseEntity<ReferralResponse> confirmSpecialty(@PathVariable UUID id,
                                                            @Valid @RequestBody ConfirmSpecialtyRequest request,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(referralService.confirmSpecialty(id, request, httpRequest.getRemoteAddr()));
    }

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/referrals/{id}/submit")
    public ResponseEntity<ReferralResponse> submit(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(referralService.submitReferral(id, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/referrals/{id}/accept")
    public ResponseEntity<ReferralResponse> accept(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(referralService.acceptReferral(id, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/referrals/{id}/complete")
    public ResponseEntity<ReferralResponse> complete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(referralService.completeReferral(id, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/referrals/{id}/cancel")
    public ResponseEntity<ReferralResponse> cancel(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(referralService.cancelReferral(id, httpRequest.getRemoteAddr()));
    }
}
