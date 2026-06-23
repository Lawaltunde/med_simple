export type AppointmentStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

// Mirrors AppointmentResponse.
export interface Appointment {
  id: string;
  patientId: string;
  physicianId: string;
  clinicId: string;
  episodeId: string | null;
  carePlanId: string | null;
  scheduledAt: string;
  durationMinutes: number;
  reason: string | null;
  status: AppointmentStatus;
  clinicName: string;
  physicianName: string;
  patientName: string;
  createdAt: string;
}
