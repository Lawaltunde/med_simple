package com.medsimple.media.controller;

import com.medsimple.media.dto.ProcessedSubmissionResponse;
import com.medsimple.media.service.MediaProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Explicit, immediate processing trigger for a submission — used by the patient intake flow
// (upload → submission → process → triage) or to process on demand without waiting for the
// poller. Ownership-gated in the service: a patient may process only their own submission.
@RestController
@RequiredArgsConstructor
public class MediaProcessingController {

    private final MediaProcessingService mediaProcessingService;

    @PostMapping("/api/v1/submissions/{submissionId}/process")
    public ResponseEntity<ProcessedSubmissionResponse> process(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(mediaProcessingService.processForCaller(submissionId));
    }
}
