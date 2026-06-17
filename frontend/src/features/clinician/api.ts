import { api } from "@/lib/api";
import type { UrgencyLevel } from "@/features/symptom/types";
import type { QueueEntry } from "./types";

export function getQueue(clinicId: string): Promise<QueueEntry[]> {
  return api<QueueEntry[]>(`/clinics/${clinicId}/queue`);
}

// Physician takes ownership of an episode (status -> ASSIGNED).
export function assignPhysician(episodeId: string, physicianId: string): Promise<QueueEntry> {
  return api<QueueEntry>(`/episodes/${episodeId}/assign`, {
    method: "POST",
    body: JSON.stringify({ physicianId }),
  });
}

export interface ConfirmUrgencyInput {
  episodeId: string;
  physicianId: string;
  confirmedUrgencyLevel: UrgencyLevel;
  overrideReason?: string;
}

// Physician confirms or overrides the AI urgency. overrideReason is required by the
// backend only when the confirmed level differs from the AI level.
export function confirmUrgency(input: ConfirmUrgencyInput): Promise<QueueEntry> {
  return api<QueueEntry>(`/episodes/${input.episodeId}/confirm-urgency`, {
    method: "POST",
    body: JSON.stringify({
      physicianId: input.physicianId,
      confirmedUrgencyLevel: input.confirmedUrgencyLevel,
      overrideReason: input.overrideReason ?? null,
    }),
  });
}
