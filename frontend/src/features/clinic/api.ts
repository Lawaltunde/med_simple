import { api } from "@/lib/api";

export type ClinicType =
  | "FAMILY_PRACTICE"
  | "WALK_IN"
  | "VIRTUAL_CARE"
  | "FAMILY_HEALTH_TEAM"
  | "SPECIALIST";

export interface Clinic {
  id: string;
  name: string;
  type: ClinicType;
  city: string | null;
  province: string | null;
}

// Public directory — used by the signup screen before the patient has an account.
export function listClinics(): Promise<Clinic[]> {
  return api<Clinic[]>("/clinics", {}, { auth: false });
}

// Clinics the authenticated staff member belongs to (empty for patients).
export function listMyClinics(): Promise<Clinic[]> {
  return api<Clinic[]>("/me/clinics");
}
