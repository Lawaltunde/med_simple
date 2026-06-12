package com.medsimple.patient.controller;

import com.medsimple.patient.dto.*;
import com.medsimple.patient.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody CreatePatientRequest request,
                                                          HttpServletRequest httpRequest) {
        PatientResponse response = patientService.createPatient(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getPatient(id));
    }

    @PatchMapping("/{id}/profile")
    public ResponseEntity<PatientResponse> updateProfile(@PathVariable UUID id,
                                                          @RequestBody UpdatePatientProfileRequest request) {
        return ResponseEntity.ok(patientService.updateProfile(id, request));
    }

    @PostMapping("/{id}/coverage")
    public ResponseEntity<HealthCoverageResponse> addCoverage(@PathVariable UUID id,
                                                               @Valid @RequestBody AddHealthCoverageRequest request,
                                                               HttpServletRequest httpRequest) {
        HealthCoverageResponse response = patientService.addHealthCoverage(id, request,
                httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/coverage")
    public ResponseEntity<List<HealthCoverageResponse>> getCoverage(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getHealthCoverage(id));
    }

    @DeleteMapping("/{id}/coverage/{coverageId}")
    public ResponseEntity<Void> deactivateCoverage(@PathVariable UUID id,
                                                    @PathVariable UUID coverageId) {
        patientService.deactivateHealthCoverage(id, coverageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/registrations")
    public ResponseEntity<ClinicRegistrationResponse> registerToClinic(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterToClinicRequest request,
            HttpServletRequest httpRequest) {
        ClinicRegistrationResponse response = patientService.registerToClinic(id, request,
                httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<List<ClinicRegistrationResponse>> getRegistrations(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getClinicRegistrations(id));
    }

    @PostMapping("/{id}/consents")
    public ResponseEntity<ConsentResponse> recordConsent(@PathVariable UUID id,
                                                          @Valid @RequestBody RecordConsentRequest request,
                                                          HttpServletRequest httpRequest) {
        ConsentResponse response = patientService.recordConsent(id, request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/consents")
    public ResponseEntity<List<ConsentResponse>> getConsents(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getConsents(id));
    }
}
