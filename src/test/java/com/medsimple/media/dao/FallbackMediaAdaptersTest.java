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
    void photoFallback_returnsUnsureWithNonBlankPlaceholder() {
        var result = new FallbackPhotoAnalysisAdapter().analyze("storage://p.jpg");
        assertThat(result.relevance()).isEqualTo(com.medsimple.symptom.entity.MediaRelevance.UNSURE);
        assertThat(result.description()).isNotBlank().contains("Photo");
    }
}
