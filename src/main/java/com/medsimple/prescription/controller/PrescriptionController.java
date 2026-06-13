package com.medsimple.prescription.controller;

import com.medsimple.prescription.dto.*;
import com.medsimple.prescription.service.PrescriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Authoring, signing, and sending prescriptions are physician-only.
@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/prescriptions")
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionRequest request,
                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionService.createPrescription(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/prescriptions/{id}")
    public ResponseEntity<PrescriptionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(prescriptionService.getPrescription(id));
    }

    @GetMapping("/api/v1/care-plans/{carePlanId}/prescriptions")
    public ResponseEntity<List<PrescriptionResponse>> getForCarePlan(@PathVariable UUID carePlanId) {
        return ResponseEntity.ok(prescriptionService.getCarePlanPrescriptions(carePlanId));
    }

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/prescriptions/{id}/sign")
    public ResponseEntity<PrescriptionResponse> sign(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(prescriptionService.signPrescription(id, httpRequest.getRemoteAddr()));
    }

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/prescriptions/{id}/send")
    public ResponseEntity<PrescriptionResponse> send(@PathVariable UUID id,
                                                     @RequestBody(required = false) SendPrescriptionRequest request,
                                                     HttpServletRequest httpRequest) {
        return ResponseEntity.ok(prescriptionService.sendPrescription(id, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/prescriptions/{id}/fill")
    public ResponseEntity<PrescriptionResponse> fill(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(prescriptionService.markFilled(id, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/prescriptions/{id}/cancel")
    public ResponseEntity<PrescriptionResponse> cancel(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(prescriptionService.cancelPrescription(id, httpRequest.getRemoteAddr()));
    }
}
