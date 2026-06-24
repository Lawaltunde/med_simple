package com.medsimple.ai.chat;

import java.util.List;

/**
 * The symptom-chat model seam: real OpenAI adapter + deterministic fallback, selected by
 * ai.chat.adapter. PHI is redacted by the service before the conversation reaches here.
 */
public interface SymptomChatDao {

    SymptomChatReply reply(List<ChatTurn> conversation);

    String adapterName();
}
