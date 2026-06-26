package com.medsimple.wearable.controller;

import com.medsimple.wearable.dto.RecordReadingsRequest;
import com.medsimple.wearable.dto.WearableTrendsResponse;
import com.medsimple.wearable.service.WearableService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Wearable / device data for a patient. Every endpoint is gated in the service: the caller must be
 * the patient or clinic staff, and the patient must hold active WEARABLE_DATA consent.
 *
 *   POST /patients/{id}/wearable/connect   → sync from the device source (synthetic in the fallback)
 *   POST /patients/{id}/wearable/readings  → manual entry
 *   POST /patients/{id}/wearable/ingest    → aggregator / webhook delivery (curl-testable)
 *   GET  /patients/{id}/wearable/trends    → trends + a PHI-safe summary
 */
@RestController
@RequiredArgsConstructor
public class WearableController {

    private final WearableService wearableService;

    @PostMapping("/api/v1/patients/{patientId}/wearable/connect")
    public ResponseEntity<WearableTrendsResponse> connect(@PathVariable UUID patientId,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(wearableService.connect(patientId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/patients/{patientId}/wearable/readings")
    public ResponseEntity<WearableTrendsResponse> recordManual(@PathVariable UUID patientId,
                                                               @Valid @RequestBody RecordReadingsRequest request,
                                                               HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wearableService.recordManual(patientId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/patients/{patientId}/wearable/ingest")
    public ResponseEntity<WearableTrendsResponse> ingest(@PathVariable UUID patientId,
                                                         @Valid @RequestBody RecordReadingsRequest request,
                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wearableService.ingest(patientId, request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/patients/{patientId}/wearable/trends")
    public ResponseEntity<WearableTrendsResponse> trends(@PathVariable UUID patientId) {
        return ResponseEntity.ok(wearableService.trends(patientId));
    }
}
