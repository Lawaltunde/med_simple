package com.medsimple.physician.controller;

import com.medsimple.physician.dto.AssignPhysicianRequest;
import com.medsimple.physician.dto.ConfirmUrgencyRequest;
import com.medsimple.physician.dto.QueueEntryResponse;
import com.medsimple.physician.service.PhysicianQueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PhysicianQueueController {

    private final PhysicianQueueService queueService;

    // The physician queue for a clinic, ordered by clinical urgency (not booking time).
    @GetMapping("/api/v1/clinics/{clinicId}/queue")
    public ResponseEntity<List<QueueEntryResponse>> getQueue(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(queueService.getQueue(clinicId));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/assign")
    public ResponseEntity<QueueEntryResponse> assignPhysician(@PathVariable UUID episodeId,
                                                              @Valid @RequestBody AssignPhysicianRequest request,
                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                queueService.assignPhysician(episodeId, request.physicianId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/confirm-urgency")
    public ResponseEntity<QueueEntryResponse> confirmUrgency(@PathVariable UUID episodeId,
                                                             @Valid @RequestBody ConfirmUrgencyRequest request,
                                                             HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                queueService.confirmUrgency(episodeId, request, httpRequest.getRemoteAddr()));
    }
}
