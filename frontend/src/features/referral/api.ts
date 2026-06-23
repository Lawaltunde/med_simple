import { api } from "@/lib/api";
import type { Referral } from "./types";

// Physician creates a referral for a care plan. Backend calls the AI specialty suggester.
export function createReferral(input: {
  carePlanId: string;
  clinicalSummary: string;
  urgency: "ROUTINE" | "URGENT" | "EMERGENT";
}): Promise<Referral> {
  return api<Referral>("/referrals", { method: "POST", body: JSON.stringify(input) });
}

export function getReferral(id: string): Promise<Referral> {
  return api<Referral>(`/referrals/${id}`);
}

// List all referrals for a care plan (physician view — by plan).
export function getCarePlanReferrals(carePlanId: string): Promise<Referral[]> {
  return api<Referral[]>(`/care-plans/${carePlanId}/referrals`);
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
