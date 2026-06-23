import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createAppointment, listClinicAppointments, listPatientAppointments } from "./api";

const POLL_MS = 30_000;

export const appointmentKeys = {
  forPatient: (patientId: string) => ["appointments", "patient", patientId] as const,
  forClinic: (clinicId: string) => ["appointments", "clinic", clinicId] as const,
};

export function usePatientAppointments(patientId: string | undefined) {
  return useQuery({
    queryKey: appointmentKeys.forPatient(patientId ?? ""),
    queryFn: () => listPatientAppointments(patientId!),
    enabled: !!patientId,
    refetchInterval: POLL_MS,
  });
}

export function useClinicAppointments(clinicId: string | undefined) {
  return useQuery({
    queryKey: appointmentKeys.forClinic(clinicId ?? ""),
    queryFn: () => listClinicAppointments(clinicId!),
    enabled: !!clinicId,
    refetchInterval: POLL_MS,
  });
}

export function useCreateAppointment(patientId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createAppointment,
    onSuccess: (appt) => {
      qc.invalidateQueries({ queryKey: appointmentKeys.forPatient(patientId) });
      qc.invalidateQueries({ queryKey: appointmentKeys.forClinic(appt.clinicId) });
    },
  });
}
