import { api } from "@/lib/api";
import type { CarePlan, CarePlanStatus, CareSuggestion, FollowUp } from "./types";

// AI care suggestion (staff-only). 204 -> no suggestion to review for this episode.
export function getEpisodeSuggestion(episodeId: string): Promise<CareSuggestion | undefined> {
  return api<CareSuggestion | undefined>(`/episodes/${episodeId}/care-suggestion`);
}

// Approve (or edit-then-approve) the suggestion into a signed, patient-visible care plan.
export function approveSuggestion(
  id: string,
  input: { physicianId: string; editedNotes?: string }
): Promise<CarePlan> {
  return api<CarePlan>(`/care-suggestions/${id}/approve`, { method: "POST", body: JSON.stringify(input) });
}

// Reject the suggestion — no care plan is created; the physician writes from scratch.
export function rejectSuggestion(id: string, input: { physicianId: string; reason?: string }): Promise<void> {
  return api<void>(`/care-suggestions/${id}/reject`, { method: "POST", body: JSON.stringify(input) });
}

export function getCarePlan(id: string): Promise<CarePlan> {
  return api<CarePlan>(`/care-plans/${id}`);
}

export function listPatientCarePlans(patientId: string): Promise<CarePlan[]> {
  return api<CarePlan[]>(`/patients/${patientId}/care-plans`);
}

// Physician authors the plan for a triaged episode (moves the episode to IN_PROGRESS).
export function createCarePlan(input: {
  episodeId: string;
  physicianId: string;
  notes: string;
}): Promise<CarePlan> {
  return api<CarePlan>("/care-plans", { method: "POST", body: JSON.stringify(input) });
}

// Physician edits notes or sets status. COMPLETED resolves the underlying episode.
export function updateCarePlan(id: string, input: { status?: CarePlanStatus; notes?: string }): Promise<CarePlan> {
  return api<CarePlan>(`/care-plans/${id}`, { method: "PATCH", body: JSON.stringify(input) });
}

// A new message on the plan. initiatedBy is the sender; the backend notifies the other side.
export function addFollowUp(
  carePlanId: string,
  input: { initiatedBy: string; message: string }
): Promise<FollowUp> {
  return api<FollowUp>(`/care-plans/${carePlanId}/follow-ups`, {
    method: "POST",
    body: JSON.stringify({ ...input, system: false }),
  });
}

// A reply to an existing message.
export function respondToFollowUp(
  carePlanId: string,
  followUpId: string,
  input: { respondedBy: string; response: string }
): Promise<FollowUp> {
  return api<FollowUp>(`/care-plans/${carePlanId}/follow-ups/${followUpId}/respond`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}
