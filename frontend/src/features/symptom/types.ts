// Mirrors the backend symptom DTOs (EpisodeResponse / SubmissionResponse) and enums.
// Timestamps are ISO-8601 strings as they arrive over JSON; parse to Date at display.

export type InputType = "TEXT" | "VOICE" | "PHOTO";

export type EpisodeStatus =
  | "SUBMITTED"
  | "AI_ANALYZED"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "EMERGENCY_DISPATCHED";

export type UrgencyLevel = "ROUTINE" | "SEE_SOON" | "URGENT" | "EMERGENCY";

export type ProcessingStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface Submission {
  id: string;
  inputType: InputType;
  textContent: string | null;
  mediaUrl: string | null;
  transcription: string | null;
  aiAnalysisResult: string | null;
  processingStatus: ProcessingStatus;
  submittedAt: string;
}

export interface Episode {
  id: string;
  patientId: string;
  assignedClinicId: string | null;
  assignedPhysicianId: string | null;
  status: EpisodeStatus;
  aiUrgencyLevel: UrgencyLevel | null;
  aiSummary: string | null;
  aiAnalyzedAt: string | null;
  confirmedUrgencyLevel: UrgencyLevel | null;
  createdAt: string;
  updatedAt: string;
  submissions: Submission[];
}

// The urgency a patient should act on: a physician's confirmation wins over the AI's guess.
export function effectiveUrgency(e: Episode): UrgencyLevel | null {
  return e.confirmedUrgencyLevel ?? e.aiUrgencyLevel;
}
