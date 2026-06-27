package com.medsimple.media.dao;

import org.springframework.stereotype.Component;

/**
 * Non-AI voice transcription — deterministic, no external calls.
 * Default adapter; active when ai.voice.adapter is not set to openai or valsea.
 * Produces a neutral placeholder so the pipeline completes without a transcription provider.
 */
@Component
public class FallbackVoiceTranscriptionAdapter implements VoiceTranscriptionDao {

    @Override
    public String transcribe(String mediaUrl) {
        return "[Voice note received. Automatic transcription is not enabled; "
                + "a clinician will review the recording.]";
    }
}
