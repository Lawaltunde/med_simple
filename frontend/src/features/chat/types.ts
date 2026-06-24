import type { UrgencyLevel } from "@/features/symptom/types";

export type ChatRole = "user" | "assistant";

export interface ChatTurn {
  role: ChatRole;
  content: string;
}

export interface SymptomChatResponse {
  reply: string;
  urgency: UrgencyLevel;
  emergency: boolean;
  redFlags: string[];
  symptomSummary: string;
  disclaimer: string;
}
