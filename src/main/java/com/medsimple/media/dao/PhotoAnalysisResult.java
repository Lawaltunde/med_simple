package com.medsimple.media.dao;

import com.medsimple.symptom.entity.MediaRelevance;

/**
 * Result of analysing a patient-submitted photo: whether it shows a medical symptom, and a factual
 * description. For a NO_SYMPTOM image the description is honest ("no visible medical symptom") rather
 * than a fabricated clinical finding.
 */
public record PhotoAnalysisResult(MediaRelevance relevance, String description) {}
