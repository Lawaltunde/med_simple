package com.medsimple.careplan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Approve (or edit-then-approve) an AI suggestion into a signed care plan.
// editedNotes null/blank => approve the AI text unchanged; otherwise the physician's edited text.
public record ApproveSuggestionRequest(
        @NotNull UUID physicianId,
        String editedNotes
) {}
