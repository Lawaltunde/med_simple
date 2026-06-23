import { api, tokenStore } from "@/lib/api";
import type { Appointment } from "./types";

export function listPatientAppointments(patientId: string): Promise<Appointment[]> {
  return api<Appointment[]>(`/patients/${patientId}/appointments`);
}

export function listClinicAppointments(clinicId: string): Promise<Appointment[]> {
  return api<Appointment[]>(`/clinics/${clinicId}/appointments`);
}

// Physician books a visit/follow-up for an episode.
export function createAppointment(input: {
  episodeId: string;
  physicianId: string;
  scheduledAt: string;
  durationMinutes?: number;
  reason?: string;
}): Promise<Appointment> {
  return api<Appointment>("/appointments", { method: "POST", body: JSON.stringify(input) });
}

// The .ics endpoint needs the auth header, so fetch it as a blob (a plain <a href> can't
// carry the token). The caller turns this into a download.
export async function downloadIcs(appointmentId: string): Promise<Blob> {
  const res = await fetch(`/api/v1/appointments/${appointmentId}/calendar.ics`, {
    credentials: "same-origin",
    headers: { Authorization: `Bearer ${tokenStore.get() ?? ""}` },
  });
  if (!res.ok) throw new Error("Could not download the calendar file");
  return res.blob();
}
