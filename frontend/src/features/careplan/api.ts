import { api } from "@/lib/api";
import type { CarePlan, CarePlanStatus, FollowUp } from "./types";

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
