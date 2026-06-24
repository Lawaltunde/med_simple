package com.medsimple.ai.chat;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.shared.privacy.SymptomRedactor;
import com.medsimple.symptom.entity.UrgencyLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomChatServiceTest {

    @Mock SymptomChatDao chatDao;
    // Real redactor so the test proves PHI actually leaves the patient turn.
    private final SymptomRedactor redactor = new SymptomRedactor();
    @Mock AccessGuard accessGuard;
    @Mock AuditService auditService;

    private SymptomChatService service() {
        return new SymptomChatService(chatDao, redactor, accessGuard, auditService);
    }

    @Test
    void redactsPhiFromPatientTurnsBeforeModel() {
        when(accessGuard.currentUserId()).thenReturn(UUID.randomUUID());
        when(chatDao.adapterName()).thenReturn("openai");
        when(chatDao.reply(any())).thenReturn(new SymptomChatReply(
                "Please see a clinician.", UrgencyLevel.SEE_SOON, List.of(), true, "summary"));

        SymptomChatRequest request = new SymptomChatRequest(List.of(
                new ChatTurn("user", "I'm John Smith, my number is 416-555-0199 and I have a cough")));

        service().chat(request, "1.2.3.4");

        ArgumentCaptor<List<ChatTurn>> sent = ArgumentCaptor.forClass(List.class);
        verify(chatDao).reply(sent.capture());
        String text = sent.getValue().get(0).content();
        assertThat(text).doesNotContain("John Smith").doesNotContain("416-555-0199");
        assertThat(text).contains("[NAME]", "[PHONE]", "cough");
    }

    @Test
    void emergencyUrgency_setsEmergencyFlag_andDisclaimerAlwaysPresent() {
        when(accessGuard.currentUserId()).thenReturn(UUID.randomUUID());
        when(chatDao.adapterName()).thenReturn("openai");
        when(chatDao.reply(any())).thenReturn(new SymptomChatReply(
                "Call 911 now.", UrgencyLevel.EMERGENCY, List.of("chest pain"), true, "summary"));

        SymptomChatResponse response = service().chat(
                new SymptomChatRequest(List.of(new ChatTurn("user", "crushing chest pain"))), "1.2.3.4");

        assertThat(response.emergency()).isTrue();
        assertThat(response.urgency()).isEqualTo(UrgencyLevel.EMERGENCY);
        assertThat(response.disclaimer()).contains("not a medical diagnosis");
    }

    @Test
    void auditsInteractionWithUrgencyOnly() {
        UUID userId = UUID.randomUUID();
        when(accessGuard.currentUserId()).thenReturn(userId);
        when(chatDao.adapterName()).thenReturn("fallback");
        when(chatDao.reply(any())).thenReturn(new SymptomChatReply(
                "ok", UrgencyLevel.ROUTINE, List.of(), true, "s"));

        service().chat(new SymptomChatRequest(List.of(new ChatTurn("user", "mild cold"))), "9.9.9.9");

        verify(auditService).log(eq(userId), eq("AI_SYMPTOM_CHAT"), eq("ai_symptom_chat"),
                eq(null), eq("9.9.9.9"), any());
    }
}
