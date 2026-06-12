package com.medsimple.ai.dao;

import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.symptom.entity.UrgencyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackAiTriageAdapterTest {

    private FallbackAiTriageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FallbackAiTriageAdapter();
    }

    @Test
    void chestPain_returnsEmergency() {
        assertThat(analyze("I have chest pain and can't breathe").urgencyLevel())
                .isEqualTo(UrgencyLevel.EMERGENCY);
    }

    @Test
    void highFever_returnsUrgent() {
        assertThat(analyze("I have a high fever and severe pain").urgencyLevel())
                .isEqualTo(UrgencyLevel.URGENT);
    }

    @Test
    void soreThroat_returnsSeesSoon() {
        assertThat(analyze("I have a sore throat and a cough").urgencyLevel())
                .isEqualTo(UrgencyLevel.SEE_SOON);
    }

    @Test
    void generalCheckup_returnsRoutine() {
        assertThat(analyze("I would like a general checkup").urgencyLevel())
                .isEqualTo(UrgencyLevel.ROUTINE);
    }

    @Test
    void emergencyKeywordBeatsLowerUrgency() {
        assertThat(analyze("sore throat and chest pain").urgencyLevel())
                .isEqualTo(UrgencyLevel.EMERGENCY);
    }

    @Test
    void summaryIsNeverBlank() {
        assertThat(analyze("some symptoms").summary()).isNotBlank();
    }

    private TriageResult analyze(String symptom) {
        return adapter.analyze(new EpisodeContext(UUID.randomUUID(), List.of(symptom)));
    }
}
