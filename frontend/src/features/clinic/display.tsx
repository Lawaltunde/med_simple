import type { CapabilityType, StaffRole, VerificationStatus } from "./api";

export const STAFF_ROLE_LABEL: Record<StaffRole, string> = {
  PHYSICIAN: "Physician",
  NURSE: "Nurse",
  RECEPTIONIST: "Receptionist",
  CLINIC_ADMIN: "Clinic admin",
};

// Each integration: what it does, and what runs when it is OFF (the built-in fallback).
export const CAPABILITY_META: Record<CapabilityType, { label: string; description: string; fallback: string }> = {
  PRESCRIBIT: {
    label: "PrescribeIT",
    description: "Electronic e-prescribing to pharmacies",
    fallback: "Signed PDF for fax or hand delivery",
  },
  OCEAN_EREFERRAL: {
    label: "Ocean eReferral",
    description: "Electronic specialist referrals (Ontario)",
    fallback: "Printable PDF referral letter",
  },
  EMR_FHIR: {
    label: "EMR connectivity (FHIR)",
    description: "Read/write to the clinic's EMR over FHIR",
    fallback: "Self-contained records in MedSimple",
  },
  LAB_ELECTRONIC: {
    label: "Electronic lab requisition",
    description: "Transmit requisitions to the lab network",
    fallback: "Printable requisition form",
  },
  LAB_BOOKING: {
    label: "Lab appointment booking",
    description: "Book the diagnostic appointment electronically",
    fallback: "Patient walks in with the requisition",
  },
  PHARMACY_ORDERING: {
    label: "Pharmacy ordering",
    description: "Dispatch the order to the pharmacy",
    fallback: "Patient brings the printed prescription",
  },
};

const VERIFICATION_STYLE: Record<VerificationStatus, { label: string; className: string }> = {
  VERIFIED: { label: "Verified", className: "bg-green-50 text-green-700" },
  PENDING: { label: "Pending", className: "bg-amber-50 text-amber-700" },
  UNVERIFIED: { label: "Unverified", className: "bg-line/60 text-muted" },
  REJECTED: { label: "Rejected", className: "bg-red-50 text-red-700" },
};

export function VerificationBadge({ status }: { status: VerificationStatus }) {
  const s = VERIFICATION_STYLE[status];
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-wide ${s.className}`}>
      {s.label}
    </span>
  );
}
