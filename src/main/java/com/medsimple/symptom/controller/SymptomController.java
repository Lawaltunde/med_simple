package com.medsimple.symptom.controller;

import com.medsimple.symptom.dto.*;
import com.medsimple.symptom.service.SymptomService;
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
public class SymptomController {

    private final SymptomService symptomService;

    // ── Patient-facing ────────────────────────────────────────────────────────

    @PostMapping("/api/v1/patients/{patientId}/episodes")
    public ResponseEntity<EpisodeResponse> startEpisode(@PathVariable UUID patientId,
                                                         @RequestBody StartEpisodeRequest request,
                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(symptomService.startEpisode(patientId, request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/patients/{patientId}/episodes")
    public ResponseEntity<List<EpisodeResponse>> getPatientEpisodes(@PathVariable UUID patientId) {
        return ResponseEntity.ok(symptomService.getPatientEpisodes(patientId));
    }

    // ── Episode-level ─────────────────────────────────────────────────────────

    @GetMapping("/api/v1/episodes/{id}")
    public ResponseEntity<EpisodeResponse> getEpisode(@PathVariable UUID id) {
        return ResponseEntity.ok(symptomService.getEpisode(id));
    }

    @PostMapping("/api/v1/episodes/{id}/submissions")
    public ResponseEntity<SubmissionResponse> addSubmission(@PathVariable UUID id,
                                                             @Valid @RequestBody AddSubmissionRequest request,
                                                             HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(symptomService.addSubmission(id, request, httpRequest.getRemoteAddr()));
    }

    // Called by AI service when processing of a voice/photo submission completes.
    @PatchMapping("/api/v1/episodes/{episodeId}/submissions/{submissionId}/status")
    public ResponseEntity<SubmissionResponse> updateSubmissionStatus(
            @PathVariable UUID episodeId,
            @PathVariable UUID submissionId,
            @Valid @RequestBody UpdateSubmissionRequest request) {
        return ResponseEntity.ok(symptomService.updateSubmissionStatus(episodeId, submissionId, request));
    }
}
