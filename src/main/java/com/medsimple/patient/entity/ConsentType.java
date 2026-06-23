package com.medsimple.patient.entity;

public enum ConsentType {
    DATA_PROCESSING,
    AI_ANALYSIS,
    PHI_SHARING,
    MARKETING,
    // Secondary use: allow this patient's de-identified care data to improve the AI
    // (eval sets, retrieval examples). Distinct from AI_ANALYSIS, which is consent to have
    // the AI assist with their own care.
    AI_TRAINING
}
