import { api } from "@/lib/api";

export type ConsentType =
  | "DATA_PROCESSING"
  | "AI_ANALYSIS"
  | "PHI_SHARING"
  | "MARKETING"
  | "AI_TRAINING"
  | "WEARABLE_DATA";

// Mirrors ConsentResponse. The consent log is append-only: each grant/revoke is a new row,
// newest first, so the first entry for a type is the current state.
export interface Consent {
  id: string;
  consentType: ConsentType;
  version: string;
  grantedAt: string | null;
  revokedAt: string | null;
  createdAt: string;
}

export function listConsents(patientId: string): Promise<Consent[]> {
  return api<Consent[]>(`/patients/${patientId}/consents`);
}

export function recordConsent(
  patientId: string,
  input: { consentType: ConsentType; version: string; grantedAt?: string; revokedAt?: string }
): Promise<Consent> {
  return api<Consent>(`/patients/${patientId}/consents`, { method: "POST", body: JSON.stringify(input) });
}
