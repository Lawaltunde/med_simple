import { api } from "@/lib/api";
import type { CreatePrescriptionInput, Prescription, SendPrescriptionInput } from "./types";

/** POST /api/v1/prescriptions — physician creates a DRAFT prescription. */
export function createPrescription(input: CreatePrescriptionInput): Promise<Prescription> {
  return api<Prescription>("/prescriptions", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

/** GET /api/v1/prescriptions/:id */
export function getPrescription(id: string): Promise<Prescription> {
  return api<Prescription>(`/prescriptions/${id}`);
}

/**
 * GET /api/v1/episodes/:episodeId/prescriptions
 * Lists all prescriptions written for an episode, newest first.
 */
export function listEpisodePrescriptions(episodeId: string): Promise<Prescription[]> {
  return api<Prescription[]>(`/episodes/${episodeId}/prescriptions`);
}

/**
 * GET /api/v1/patients/:patientId/prescriptions
 * Lists all prescriptions for a patient across all care plans, newest first.
 */
export function listPatientPrescriptions(patientId: string): Promise<Prescription[]> {
  return api<Prescription[]>(`/patients/${patientId}/prescriptions`);
}

/** POST /api/v1/prescriptions/:id/sign — physician signs the DRAFT. */
export function signPrescription(id: string): Promise<Prescription> {
  return api<Prescription>(`/prescriptions/${id}/sign`, { method: "POST" });
}

/** POST /api/v1/prescriptions/:id/send — delivers via PrescribeIT or PDF fallback. */
export function sendPrescription(id: string, input?: SendPrescriptionInput): Promise<Prescription> {
  return api<Prescription>(`/prescriptions/${id}/send`, {
    method: "POST",
    body: JSON.stringify(input ?? {}),
  });
}

/** POST /api/v1/prescriptions/:id/cancel */
export function cancelPrescription(id: string): Promise<Prescription> {
  return api<Prescription>(`/prescriptions/${id}/cancel`, { method: "POST" });
}
