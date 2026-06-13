package com.medsimple.diagnostic.controller;

import com.medsimple.diagnostic.dto.*;
import com.medsimple.diagnostic.service.DiagnosticService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DiagnosticController {

    private final DiagnosticService diagnosticService;

    // ── Requisitions ────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/requisitions")
    public ResponseEntity<RequisitionResponse> createRequisition(
            @Valid @RequestBody CreateRequisitionRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diagnosticService.createRequisition(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/requisitions/{id}")
    public ResponseEntity<RequisitionResponse> getRequisition(@PathVariable UUID id) {
        return ResponseEntity.ok(diagnosticService.getRequisition(id));
    }

    @GetMapping("/api/v1/care-plans/{carePlanId}/requisitions")
    public ResponseEntity<List<RequisitionResponse>> getForCarePlan(@PathVariable UUID carePlanId) {
        return ResponseEntity.ok(diagnosticService.getCarePlanRequisitions(carePlanId));
    }

    @PostMapping("/api/v1/requisitions/{id}/send")
    public ResponseEntity<RequisitionResponse> sendRequisition(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(diagnosticService.sendRequisition(id, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/requisitions/{id}/book")
    public ResponseEntity<RequisitionResponse> bookRequisition(
            @PathVariable UUID id, @Valid @RequestBody BookRequisitionRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(diagnosticService.bookRequisition(id, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/requisitions/{id}/cancel")
    public ResponseEntity<RequisitionResponse> cancelRequisition(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(diagnosticService.cancelRequisition(id, httpRequest.getRemoteAddr()));
    }

    // ── Results: physician side ─────────────────────────────────────────────────

    @PostMapping("/api/v1/requisitions/{requisitionId}/result")
    public ResponseEntity<PhysicianResultResponse> recordResult(
            @PathVariable UUID requisitionId, @Valid @RequestBody RecordResultRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diagnosticService.recordResult(requisitionId, request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/results/{id}")
    public ResponseEntity<PhysicianResultResponse> getResultForPhysician(@PathVariable UUID id) {
        return ResponseEntity.ok(diagnosticService.getResultForPhysician(id));
    }

    @PostMapping("/api/v1/results/{id}/interpretation")
    public ResponseEntity<PhysicianResultResponse> addInterpretation(
            @PathVariable UUID id, @Valid @RequestBody AddInterpretationRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(diagnosticService.addInterpretation(id, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/results/{id}/release")
    public ResponseEntity<PhysicianResultResponse> releaseResult(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(diagnosticService.releaseResult(id, httpRequest.getRemoteAddr()));
    }

    // ── Results: patient side (release-gated) ───────────────────────────────────

    @GetMapping("/api/v1/patients/{patientId}/results")
    public ResponseEntity<List<PatientResultResponse>> getPatientResults(@PathVariable UUID patientId) {
        return ResponseEntity.ok(diagnosticService.getPatientReleasedResults(patientId));
    }

    @GetMapping("/api/v1/patient-results/{id}")
    public ResponseEntity<PatientResultResponse> getPatientResult(@PathVariable UUID id) {
        return ResponseEntity.ok(diagnosticService.getResultForPatient(id));
    }
}
