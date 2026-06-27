package com.medsimple.media.dao;

import com.medsimple.symptom.entity.MediaRelevance;
import org.springframework.stereotype.Component;

/**
 * Non-AI photo analysis — deterministic, no external calls.
 * Default adapter; active when ai.photo.adapter is not set to openai.
 */
@Component
public class FallbackPhotoAnalysisAdapter implements PhotoAnalysisDao {

    @Override
    public PhotoAnalysisResult analyze(String mediaUrl) {
        // No AI to judge relevance — UNSURE, so the image still reaches a clinician, flagged.
        return new PhotoAnalysisResult(MediaRelevance.UNSURE,
                "[Photo received. Automatic image analysis is not enabled; a clinician will review the image.]");
    }
}
