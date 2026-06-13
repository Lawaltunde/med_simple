package com.medsimple.media.dao;

public interface VoiceTranscriptionDao {

    // Transcribes the audio at mediaUrl to text. PHI: the audio may contain identity;
    // transcription output is treated as symptom text and stays within the platform.
    String transcribe(String mediaUrl);
}
