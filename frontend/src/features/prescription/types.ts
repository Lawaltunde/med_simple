// Mirrors PrescriptionResponse, PrescriptionStatus, DeliveryMethod from the backend.

export type PrescriptionStatus =
  | "DRAFT"
  | "SIGNED"
  | "SENT_ELECTRONIC"
  | "SENT_PDF"
  | "FILLED"
  | "CANCELLED";

export type DeliveryMethod = "PRESCRIBIT" | "PDF_FAX" | "PDF_PRINT";

export interface Prescription {
  id: string;
  carePlanId: string;
  patientId: string;
  prescribingPhysicianId: string;
  medicationName: string;
  medicationCode: string | null;
  dosage: string;
  frequency: string;
  duration: string | null;
  refills: number;
  instructions: string | null;
  /** Raw JSON from the AI safety check: { interactions, allergyFlags, doseWarnings } */
  aiSafetyChecks: string | null;
  status: PrescriptionStatus;
  deliveryMethod: DeliveryMethod | null;
  pharmacyId: string | null;
  documentUrl: string | null;
  signedAt: string | null;
  sentAt: string | null;
  filledAt: string | null;
  createdAt: string;
}

// Mirrors CreatePrescriptionRequest
export interface CreatePrescriptionInput {
  carePlanId: string;
  medicationName: string;
  medicationCode?: string;
  dosage: string;
  frequency: string;
  duration?: string;
  refills?: number;
  instructions?: string;
}

// Mirrors SendPrescriptionRequest (pharmacyId is optional)
export interface SendPrescriptionInput {
  pharmacyId?: string;
}

// Parsed shape of aiSafetyChecks JSON
export interface AiSafetyResult {
  interactions: string[];
  allergyFlags: string[];
  doseWarnings: string[];
}
