// Mirrors the backend CarePlanResponse / FollowUpResponse.

export type CarePlanStatus = "ACTIVE" | "COMPLETED" | "CANCELLED";

export interface FollowUp {
  id: string;
  initiatedBy: string | null; // userId of the asker (null for a system prompt)
  system: boolean;
  message: string;
  response: string | null;
  respondedBy: string | null;
  respondedAt: string | null;
  createdAt: string;
}

export interface CarePlan {
  id: string;
  episodeId: string;
  patientId: string;
  physicianId: string;
  clinicId: string;
  status: CarePlanStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  followUps: FollowUp[];
}

// AI-proposed plan of care (staff-only). The patient never sees this — only a
// physician-approved care plan reaches them.
export type CareSuggestionStatus = "PROPOSED" | "APPROVED" | "EDITED" | "REJECTED" | "FAILED";

export interface CareSuggestion {
  id: string;
  episodeId: string;
  status: CareSuggestionStatus;
  suggestedPlan: string | null;
  rationale: string | null;
  suggestedFollowUpDays: number | null;
  adapter: string;
  model: string | null;
  createdAt: string;
}
