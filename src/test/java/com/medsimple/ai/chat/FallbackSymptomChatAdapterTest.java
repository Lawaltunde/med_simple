package com.medsimple.ai.chat;

import com.medsimple.symptom.entity.UrgencyLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackSymptomChatAdapterTest {

    private final FallbackSymptomChatAdapter adapter = new FallbackSymptomChatAdapter();

    @Test
    void escalatesOnEmergencyKeyword() {
        SymptomChatReply reply = adapter.reply(List.of(
                new ChatTurn("user", "I have crushing chest pain and can't breathe")));

        assertThat(reply.urgency()).isEqualTo(UrgencyLevel.EMERGENCY);
        assertThat(reply.reply()).containsIgnoringCase("911");
        assertThat(reply.recommendSeeClinician()).isTrue();
    }

    @Test
    void normalSymptom_recommendsClinician_neverDiagnoses() {
        SymptomChatReply reply = adapter.reply(List.of(
                new ChatTurn("user", "I have a mild sore throat for a day")));

        assertThat(reply.urgency()).isEqualTo(UrgencyLevel.SEE_SOON);
        assertThat(reply.recommendSeeClinician()).isTrue();
        assertThat(reply.reply()).containsIgnoringCase("clinician");
        // Never asserts a diagnosis.
        assertThat(reply.reply().toLowerCase()).doesNotContain("you have");
    }
}
