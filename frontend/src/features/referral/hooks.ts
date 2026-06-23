import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelReferral,
  confirmSpecialty,
  createReferral,
  getCarePlanReferrals,
  getPatientReferrals,
  submitReferral,
} from "./api";
import type { ReferralUrgency } from "./types";

export const referralKeys = {
  forCarePlan: (carePlanId: string) => ["referrals", "care-plan", carePlanId] as const,
  forPatient: (patientId: string) => ["referrals", "patient", patientId] as const,
  detail: (id: string) => ["referrals", "detail", id] as const,
};

// Patient polling: 30 s so status changes (SUBMITTED → ACCEPTED → COMPLETED) surface without refresh.
const PATIENT_POLL_MS = 30_000;

export function useCarePlanReferrals(carePlanId: string | undefined) {
  return useQuery({
    queryKey: referralKeys.forCarePlan(carePlanId ?? ""),
    queryFn: () => getCarePlanReferrals(carePlanId!),
    enabled: !!carePlanId,
  });
}

export function usePatientReferrals(patientId: string | undefined) {
  return useQuery({
    queryKey: referralKeys.forPatient(patientId ?? ""),
    queryFn: () => getPatientReferrals(patientId!),
    enabled: !!patientId,
    refetchInterval: PATIENT_POLL_MS,
  });
}

function useReferralInvalidation() {
  const qc = useQueryClient();
  return (opts: { carePlanId?: string; patientId?: string; referralId?: string }) => {
    if (opts.carePlanId) qc.invalidateQueries({ queryKey: referralKeys.forCarePlan(opts.carePlanId) });
    if (opts.patientId) qc.invalidateQueries({ queryKey: referralKeys.forPatient(opts.patientId) });
    if (opts.referralId) qc.invalidateQueries({ queryKey: referralKeys.detail(opts.referralId) });
  };
}

export function useCreateReferral(carePlanId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: (input: { clinicalSummary: string; urgency: ReferralUrgency }) =>
      createReferral({ carePlanId, ...input }),
    onSuccess: (r) => invalidate({ carePlanId, patientId: r.patientId }),
  });
}

export function useConfirmSpecialty(carePlanId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: ({ id, confirmedSpecialty }: { id: string; confirmedSpecialty: string }) =>
      confirmSpecialty(id, confirmedSpecialty),
    onSuccess: (r) => invalidate({ carePlanId, patientId: r.patientId, referralId: r.id }),
  });
}

export function useSubmitReferral(carePlanId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: (id: string) => submitReferral(id),
    onSuccess: (r) => invalidate({ carePlanId, patientId: r.patientId, referralId: r.id }),
  });
}

export function useCancelReferral(carePlanId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: (id: string) => cancelReferral(id),
    onSuccess: (r) => invalidate({ carePlanId, patientId: r.patientId, referralId: r.id }),
  });
}
