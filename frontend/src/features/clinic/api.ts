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

// ── Clinic administration (CLINIC_ADMIN / SUPER_ADMIN) ─────────────────────────

export type StaffRole = "PHYSICIAN" | "NURSE" | "RECEPTIONIST" | "CLINIC_ADMIN";
export type VerificationStatus = "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED";
export type CapabilityType =
  | "PRESCRIBIT"
  | "OCEAN_EREFERRAL"
  | "EMR_FHIR"
  | "LAB_ELECTRONIC"
  | "LAB_BOOKING"
  | "PHARMACY_ORDERING";

export interface Staff {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: StaffRole;
  licenseNumber: string | null;
  specialization: string | null;
  college: string | null;
  registrationNumber: string | null;
  licenseProvince: string | null;
  billingNumber: string | null;
  verificationStatus: VerificationStatus;
  verificationSource: string | null;
  verifiedAt: string | null;
  active: boolean;
  createdAt: string;
}

export interface Capability {
  id: string;
  capability: CapabilityType;
  enabled: boolean;
}

export interface AddStaffInput {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: StaffRole;
  licenseNumber?: string;
  specialization?: string;
  college?: string;
  registrationNumber?: string;
  licenseProvince?: string;
  billingNumber?: string;
}

export function listStaff(clinicId: string): Promise<Staff[]> {
  return api<Staff[]>(`/clinics/${clinicId}/staff`);
}

export function addStaff(clinicId: string, input: AddStaffInput): Promise<Staff> {
  return api<Staff>(`/clinics/${clinicId}/staff`, { method: "POST", body: JSON.stringify(input) });
}

export function removeStaff(clinicId: string, staffId: string): Promise<void> {
  return api<void>(`/clinics/${clinicId}/staff/${staffId}`, { method: "DELETE" });
}

// Run College-registration verification on a staff member (physicians).
export function verifyStaff(clinicId: string, staffId: string): Promise<Staff> {
  return api<Staff>(`/clinics/${clinicId}/staff/${staffId}/verify`, { method: "POST" });
}

export function listCapabilities(clinicId: string): Promise<Capability[]> {
  return api<Capability[]>(`/clinics/${clinicId}/capabilities`);
}

export function setCapability(clinicId: string, capability: CapabilityType, enabled: boolean): Promise<Capability> {
  return api<Capability>(`/clinics/${clinicId}/capabilities`, {
    method: "POST",
    body: JSON.stringify({ capability, enabled }),
  });
}
