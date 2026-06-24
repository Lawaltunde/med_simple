import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelPrescription,
  createPrescription,
  listEpisodePrescriptions,
  listPatientPrescriptions,
  sendPrescription,
  signPrescription,
} from "./api";
import type { CreatePrescriptionInput, SendPrescriptionInput } from "./types";

const POLL_MS = 30_000;

export const prescriptionKeys = {
  forEpisode: (episodeId: string) => ["prescriptions", "episode", episodeId] as const,
  forPatient: (patientId: string) => ["prescriptions", "patient", patientId] as const,
  detail: (id: string) => ["prescriptions", "detail", id] as const,
};

/** Physician view — prescriptions scoped to a single episode. */
export function useEpisodePrescriptions(episodeId: string | undefined) {
  return useQuery({
    queryKey: prescriptionKeys.forEpisode(episodeId ?? ""),
    queryFn: () => listEpisodePrescriptions(episodeId!),
    enabled: !!episodeId,
    refetchInterval: POLL_MS,
  });
}

/** Patient view — all prescriptions for the patient, newest first. */
export function usePatientPrescriptions(patientId: string | undefined) {
  return useQuery({
    queryKey: prescriptionKeys.forPatient(patientId ?? ""),
    queryFn: () => listPatientPrescriptions(patientId!),
    enabled: !!patientId,
    refetchInterval: POLL_MS,
  });
}

function usePrescriptionInvalidation() {
  const qc = useQueryClient();
  return (opts: { episodeId?: string; patientId?: string; prescriptionId?: string }) => {
    if (opts.episodeId) qc.invalidateQueries({ queryKey: prescriptionKeys.forEpisode(opts.episodeId) });
    if (opts.patientId) qc.invalidateQueries({ queryKey: prescriptionKeys.forPatient(opts.patientId) });
    if (opts.prescriptionId) qc.invalidateQueries({ queryKey: prescriptionKeys.detail(opts.prescriptionId) });
  };
}

export function useCreatePrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: createPrescription,
    onSuccess: (rx) => invalidate({ episodeId: rx.episodeId, patientId: rx.patientId }),
  });
}

export function useSignPrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: (id: string) => signPrescription(id),
    onSuccess: (rx) =>
      invalidate({ episodeId: rx.episodeId, patientId: rx.patientId, prescriptionId: rx.id }),
  });
}

export function useSendPrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input?: SendPrescriptionInput }) =>
      sendPrescription(id, input),
    onSuccess: (rx) =>
      invalidate({ episodeId: rx.episodeId, patientId: rx.patientId, prescriptionId: rx.id }),
  });
}

export function useCancelPrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: (id: string) => cancelPrescription(id),
    onSuccess: (rx) =>
      invalidate({ episodeId: rx.episodeId, patientId: rx.patientId, prescriptionId: rx.id }),
  });
}

// Re-export input types consumed by the mutate() callers
export type { CreatePrescriptionInput, SendPrescriptionInput };
