// Mirrors the backend diagnostic DTOs and enums.
// Timestamps are ISO-8601 strings as they arrive over JSON.

export type RequisitionStatus =
  | "DRAFT"
  | "SENT"
  | "BOOKED"
  | "IN_PROGRESS"
  | "RESULTED"
  | "CANCELLED";

export type RequisitionUrgency = "ROUTINE" | "URGENT" | "STAT";

export type DiagnosticDeliveryMethod = "ELECTRONIC" | "PDF_PRINT";

// Mirrors RequisitionResponse.
export interface Requisition {
  id: string;
  episodeId: string;
  carePlanId: string | null;
  patientId: string;
  orderingPhysicianId: string;
  testType: string;
  testCodes: string[] | null;
  clinicalIndication: string | null;
  urgency: RequisitionUrgency;
  status: RequisitionStatus;
  deliveryMethod: DiagnosticDeliveryMethod | null;
  diagnosticCentreId: string | null;
  documentUrl: string | null;
  appointmentAt: string | null;
  sentAt: string | null;
  bookedAt: string | null;
  createdAt: string;
}

// Mirrors PhysicianResultResponse — full view including raw data.
export interface PhysicianResult {
  id: string;
  requisitionId: string;
  patientId: string;
  orderingPhysicianId: string;
  rawResultText: string;
  rawResultData: string | null;
  aiSummary: string | null;
  physicianInterpretation: string | null;
  releasedToPatient: boolean;
  releasedToPatientAt: string | null;
  receivedAt: string;
}

// Mirrors PatientResultResponse — safe subset shown after release.
// Raw lab data intentionally excluded; patient sees AI summary and physician interpretation.
export interface PatientResult {
  id: string;
  requisitionId: string;
  testType: string;
  aiSummary: string | null;
  physicianInterpretation: string | null;
  releasedToPatientAt: string;
}
