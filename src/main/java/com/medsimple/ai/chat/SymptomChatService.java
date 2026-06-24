package com.medsimple.ai.chat;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.shared.privacy.SymptomRedactor;
import com.medsimple.symptom.entity.UrgencyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Patient-facing symptom assistant. Compliance is enforced here, not left to the model:
 *
 * <ol>
 *   <li>PHI is redacted from every patient message before the conversation reaches the model
 *       (CLAUDE.md: PHI stripped before any AI provider).</li>
 *   <li>Emergency escalation is deterministic — driven by the model's structured urgency, not by
 *       parsing prose — so the UI can show a 911 banner reliably.</li>
 *   <li>Every interaction is audited (urgency only, never the message text).</li>
 *   <li>A standing disclaimer ("information, not a diagnosis") is always returned.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SymptomChatService {

    static final String DISCLAIMER =
            "This is general health information from an AI assistant, not a medical diagnosis. "
                    + "Always confirm with a clinician. If this is an emergency, call 911.";

    private final SymptomChatDao chatDao;
    private final SymptomRedactor symptomRedactor;
    private final AccessGuard accessGuard;
    private final AuditService auditService;

    public SymptomChatResponse chat(SymptomChatRequest request, String ipAddress) {
        // Redact PHI from the patient's turns before anything leaves for the model. Assistant turns
        // are our own prior replies and carry no new PHI.
        List<ChatTurn> redacted = request.messages().stream()
                .map(turn -> "assistant".equalsIgnoreCase(turn.role())
                        ? turn
                        : new ChatTurn("user", symptomRedactor.redact(turn.content())))
                .toList();

        SymptomChatReply reply = chatDao.reply(redacted);
        boolean emergency = reply.urgency() == UrgencyLevel.EMERGENCY;

        auditService.log(accessGuard.currentUserId(), "AI_SYMPTOM_CHAT", "ai_symptom_chat", null,
                ipAddress, Map.of("urgency", reply.urgency().name(), "adapter", chatDao.adapterName()));

        return new SymptomChatResponse(
                reply.reply(),
                reply.urgency(),
                emergency,
                reply.redFlags() == null ? List.of() : reply.redFlags(),
                reply.symptomSummary(),
                DISCLAIMER);
    }
}
