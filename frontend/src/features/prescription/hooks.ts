import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelPrescription,
  createPrescription,
  listCarePlanPrescriptions,
  listPatientPrescriptions,
  sendPrescription,
  signPrescription,
} from "./api";
import type { CreatePrescriptionInput, SendPrescriptionInput } from "./types";

const POLL_MS = 30_000;

export const prescriptionKeys = {
  forCarePlan: (carePlanId: string) => ["prescriptions", "care-plan", carePlanId] as const,
  forPatient: (patientId: string) => ["prescriptions", "patient", patientId] as const,
  detail: (id: string) => ["prescriptions", "detail", id] as const,
};

/** Physician view — prescriptions scoped to a single care plan. */
export function useCarePlanPrescriptions(carePlanId: string | undefined) {
  return useQuery({
    queryKey: prescriptionKeys.forCarePlan(carePlanId ?? ""),
    queryFn: () => listCarePlanPrescriptions(carePlanId!),
    enabled: !!carePlanId,
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
  return (opts: { carePlanId?: string; patientId?: string; prescriptionId?: string }) => {
    if (opts.carePlanId) qc.invalidateQueries({ queryKey: prescriptionKeys.forCarePlan(opts.carePlanId) });
    if (opts.patientId) qc.invalidateQueries({ queryKey: prescriptionKeys.forPatient(opts.patientId) });
    if (opts.prescriptionId) qc.invalidateQueries({ queryKey: prescriptionKeys.detail(opts.prescriptionId) });
  };
}

export function useCreatePrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: createPrescription,
    onSuccess: (rx) => invalidate({ carePlanId: rx.carePlanId, patientId: rx.patientId }),
  });
}

export function useSignPrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: (id: string) => signPrescription(id),
    onSuccess: (rx) =>
      invalidate({ carePlanId: rx.carePlanId, patientId: rx.patientId, prescriptionId: rx.id }),
  });
}

export function useSendPrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input?: SendPrescriptionInput }) =>
      sendPrescription(id, input),
    onSuccess: (rx) =>
      invalidate({ carePlanId: rx.carePlanId, patientId: rx.patientId, prescriptionId: rx.id }),
  });
}

export function useCancelPrescription() {
  const invalidate = usePrescriptionInvalidation();
  return useMutation({
    mutationFn: (id: string) => cancelPrescription(id),
    onSuccess: (rx) =>
      invalidate({ carePlanId: rx.carePlanId, patientId: rx.patientId, prescriptionId: rx.id }),
  });
}

// Re-export input types consumed by the mutate() callers
export type { CreatePrescriptionInput, SendPrescriptionInput };
