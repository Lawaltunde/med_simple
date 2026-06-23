package com.medsimple.careplan.controller;

import com.medsimple.careplan.dto.ApproveSuggestionRequest;
import com.medsimple.careplan.dto.CarePlanResponse;
import com.medsimple.careplan.dto.CareSuggestionResponse;
import com.medsimple.careplan.dto.RejectSuggestionRequest;
import com.medsimple.careplan.service.CarePlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// AI care suggestions are staff-only. There is deliberately NO patient route here — the
// patient-visibility gate is structural (a patient can only read care_plans).
@RestController
@RequiredArgsConstructor
public class CareSuggestionController {

    private final CarePlanService carePlanService;

    // The PROPOSED suggestion a physician would review for an episode. 204 when there is none
    // (older episode, EMERGENCY-suppressed, guardrail-blocked, or already actioned).
    @GetMapping("/api/v1/episodes/{episodeId}/care-suggestion")
    public ResponseEntity<CareSuggestionResponse> getForEpisode(@PathVariable UUID episodeId) {
        return carePlanService.getSuggestionForEpisode(episodeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // Approve (or edit-then-approve) -> creates the signed, patient-visible care plan.
    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/care-suggestions/{id}/approve")
    public ResponseEntity<CarePlanResponse> approve(@PathVariable UUID id,
                                                    @Valid @RequestBody ApproveSuggestionRequest request,
                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.createFromSuggestion(id, request, httpRequest.getRemoteAddr()));
    }

    // Reject -> no care plan; the physician writes from scratch.
    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/care-suggestions/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id,
                                       @Valid @RequestBody RejectSuggestionRequest request,
                                       HttpServletRequest httpRequest) {
        carePlanService.rejectSuggestion(id, request, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
