import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelReferral,
  confirmSpecialty,
  createReferral,
  getEpisodeReferrals,
  getPatientReferrals,
  submitReferral,
} from "./api";
import type { ReferralUrgency } from "./types";

export const referralKeys = {
  forEpisode: (episodeId: string) => ["referrals", "episode", episodeId] as const,
  forPatient: (patientId: string) => ["referrals", "patient", patientId] as const,
  detail: (id: string) => ["referrals", "detail", id] as const,
};

// Patient polling: 30 s so status changes (SUBMITTED → ACCEPTED → COMPLETED) surface without refresh.
const PATIENT_POLL_MS = 30_000;

export function useEpisodeReferrals(episodeId: string | undefined) {
  return useQuery({
    queryKey: referralKeys.forEpisode(episodeId ?? ""),
    queryFn: () => getEpisodeReferrals(episodeId!),
    enabled: !!episodeId,
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
  return (opts: { episodeId?: string; patientId?: string; referralId?: string }) => {
    if (opts.episodeId) qc.invalidateQueries({ queryKey: referralKeys.forEpisode(opts.episodeId) });
    if (opts.patientId) qc.invalidateQueries({ queryKey: referralKeys.forPatient(opts.patientId) });
    if (opts.referralId) qc.invalidateQueries({ queryKey: referralKeys.detail(opts.referralId) });
  };
}

export function useCreateReferral(episodeId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: (input: { clinicalSummary: string; urgency: ReferralUrgency }) =>
      createReferral({ episodeId, ...input }),
    onSuccess: (r) => invalidate({ episodeId, patientId: r.patientId }),
  });
}

export function useConfirmSpecialty(episodeId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: ({ id, confirmedSpecialty }: { id: string; confirmedSpecialty: string }) =>
      confirmSpecialty(id, confirmedSpecialty),
    onSuccess: (r) => invalidate({ episodeId, patientId: r.patientId, referralId: r.id }),
  });
}

export function useSubmitReferral(episodeId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: (id: string) => submitReferral(id),
    onSuccess: (r) => invalidate({ episodeId, patientId: r.patientId, referralId: r.id }),
  });
}

export function useCancelReferral(episodeId: string) {
  const invalidate = useReferralInvalidation();
  return useMutation({
    mutationFn: (id: string) => cancelReferral(id),
    onSuccess: (r) => invalidate({ episodeId, patientId: r.patientId, referralId: r.id }),
  });
}
