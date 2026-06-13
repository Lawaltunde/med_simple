package com.medsimple.media.dao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackMediaAdaptersTest {

    @Test
    void voiceFallback_returnsNonBlankPlaceholder() {
        String result = new FallbackVoiceTranscriptionAdapter().transcribe("storage://v.mp3");
        assertThat(result).isNotBlank().contains("Voice note");
    }

    @Test
    void photoFallback_returnsNonBlankPlaceholder() {
        String result = new FallbackPhotoAnalysisAdapter().analyze("storage://p.jpg");
        assertThat(result).isNotBlank().contains("Photo");
    }
}
