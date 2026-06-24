import { api } from "@/lib/api";
import type { ChatTurn, SymptomChatResponse } from "./types";

// Patient-facing AI symptom assistant. PHI is redacted server-side before the model.
export function sendSymptomChat(messages: ChatTurn[]): Promise<SymptomChatResponse> {
  return api<SymptomChatResponse>("/ai/symptom-chat", {
    method: "POST",
    body: JSON.stringify({ messages }),
  });
}
