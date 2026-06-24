import { api } from "@/lib/api";
import type { Referral } from "./types";

// Physician refers off the episode (no care plan needed). Backend calls the AI specialty suggester.
export function createReferral(input: {
  episodeId: string;
  clinicalSummary: string;
  urgency: "ROUTINE" | "URGENT" | "EMERGENT";
}): Promise<Referral> {
  return api<Referral>("/referrals", { method: "POST", body: JSON.stringify(input) });
}

export function getReferral(id: string): Promise<Referral> {
  return api<Referral>(`/referrals/${id}`);
}

// List all referrals for an episode (physician/console view).
export function getEpisodeReferrals(episodeId: string): Promise<Referral[]> {
  return api<Referral[]>(`/episodes/${episodeId}/referrals`);
}

// List all referrals for a patient (patient view — newest first).
export function getPatientReferrals(patientId: string): Promise<Referral[]> {
  return api<Referral[]>(`/patients/${patientId}/referrals`);
}

// Physician confirms or overrides the AI specialty suggestion. Only valid on a DRAFT referral.
export function confirmSpecialty(id: string, confirmedSpecialty: string): Promise<Referral> {
  return api<Referral>(`/referrals/${id}/confirm-specialty`, {
    method: "POST",
    body: JSON.stringify({ confirmedSpecialty }),
  });
}

// Deliver the referral via Ocean eReferral or PDF (decided by clinic_capabilities at the backend).
export function submitReferral(id: string): Promise<Referral> {
  return api<Referral>(`/referrals/${id}/submit`, { method: "POST" });
}

export function cancelReferral(id: string): Promise<Referral> {
  return api<Referral>(`/referrals/${id}/cancel`, { method: "POST" });
}
