package com.medsimple.ai.chat;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** The conversation so far. The last turn is the patient's new message. */
public record SymptomChatRequest(
        @NotEmpty List<ChatTurn> messages
) {}
