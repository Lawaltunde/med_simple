package com.medsimple.ai.controller;

import com.medsimple.ai.service.AiTriageService;
import com.medsimple.symptom.dto.EpisodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/episodes")
@RequiredArgsConstructor
public class AiTriageController {

    private final AiTriageService aiTriageService;

    @PostMapping("/{id}/triage")
    public ResponseEntity<EpisodeResponse> triageEpisode(@PathVariable UUID id) {
        return ResponseEntity.ok(aiTriageService.triageEpisode(id));
    }
}
