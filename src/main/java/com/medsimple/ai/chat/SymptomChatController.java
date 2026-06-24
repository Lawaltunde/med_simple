package com.medsimple.ai.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Patient-facing symptom assistant. Behind the JWT filter (authenticated patients); the service
 * redacts PHI before the model and escalates emergencies deterministically.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class SymptomChatController {

    private final SymptomChatService chatService;

    @PostMapping("/symptom-chat")
    public ResponseEntity<SymptomChatResponse> chat(@Valid @RequestBody SymptomChatRequest request,
                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(chatService.chat(request, httpRequest.getRemoteAddr()));
    }
}
