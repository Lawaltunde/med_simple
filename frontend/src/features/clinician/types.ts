import type { EpisodeStatus, UrgencyLevel } from "@/features/symptom/types";

// Mirrors the backend QueueEntryResponse. effectiveUrgency drives queue ordering:
// the physician-confirmed level if set, otherwise the AI-assessed level.
export interface QueueEntry {
  episodeId: string;
  patientId: string;
  patientFirstName: string;
  patientLastName: string;
  effectiveUrgency: UrgencyLevel | null;
  aiUrgencyLevel: UrgencyLevel | null;
  confirmedUrgencyLevel: UrgencyLevel | null;
  status: EpisodeStatus;
  aiSummary: string | null;
  assignedPhysicianId: string | null;
  createdAt: string;
}
