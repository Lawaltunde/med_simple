package com.medsimple.media.dao;

import org.springframework.stereotype.Component;

/**
 * Non-AI photo analysis — deterministic, no external calls.
 * Default adapter; active when ai.media.adapter is not set to openai.
 */
@Component
public class FallbackPhotoAnalysisAdapter implements PhotoAnalysisDao {

    @Override
    public String analyze(String mediaUrl) {
        return "[Photo received. Automatic image analysis is not enabled; "
                + "a clinician will review the image.]";
    }
}
