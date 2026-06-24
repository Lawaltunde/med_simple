// Mirrors the backend ReferralResponse / enums exactly.

export type ReferralStatus = "DRAFT" | "SUBMITTED" | "ACCEPTED" | "COMPLETED" | "CANCELLED";
export type ReferralUrgency = "ROUTINE" | "URGENT" | "EMERGENT";
export type ReferralDeliveryMethod = "OCEAN_EREFERRAL" | "PDF_FAX" | "PDF_PRINT";

export interface Referral {
  id: string;
  episodeId: string;
  carePlanId: string | null;
  patientId: string;
  referringPhysicianId: string;
  aiSuggestedSpecialty: string;
  confirmedSpecialty: string | null;
  clinicalSummary: string;
  urgency: ReferralUrgency;
  status: ReferralStatus;
  deliveryMethod: ReferralDeliveryMethod | null;
  externalReferralId: string | null;
  documentUrl: string | null;
  submittedAt: string | null;
  createdAt: string;
}
