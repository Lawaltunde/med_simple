package com.medsimple.prescription.dao;

import java.util.List;

// Mirrors the ai_safety_checks JSONB shape: {interactions, allergyFlags, doseWarnings}.
public record SafetyCheckResult(
        List<String> interactions,
        List<String> allergyFlags,
        List<String> doseWarnings
) {
    public boolean hasFindings() {
        return !interactions.isEmpty() || !allergyFlags.isEmpty() || !doseWarnings.isEmpty();
    }
}
