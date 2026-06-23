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
