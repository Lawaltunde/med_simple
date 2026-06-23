import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { episodeKeys } from "@/features/symptom/hooks";
import {
  addFollowUp,
  createCarePlan,
  getCarePlan,
  listPatientCarePlans,
  respondToFollowUp,
  updateCarePlan,
} from "./api";
import type { CarePlanStatus } from "./types";

export const carePlanKeys = {
  forPatient: (patientId: string) => ["care-plans", "patient", patientId] as const,
  detail: (id: string) => ["care-plans", "detail", id] as const,
};

// Poll so a newly created plan / new message appears without a manual refresh.
const POLL_MS = 30_000;

export function usePatientCarePlans(patientId: string | undefined) {
  return useQuery({
    queryKey: carePlanKeys.forPatient(patientId ?? ""),
    queryFn: () => listPatientCarePlans(patientId!),
    enabled: !!patientId,
    refetchInterval: POLL_MS,
  });
}

export function useCarePlan(id: string | undefined) {
  return useQuery({
    queryKey: carePlanKeys.detail(id ?? ""),
    queryFn: () => getCarePlan(id!),
    enabled: !!id,
    refetchInterval: POLL_MS,
  });
}

// After any care-plan write, refresh the plan lists, the plan detail, and the episode
// (its status moves IN_PROGRESS / RESOLVED, which also reorders the physician queue).
function useCarePlanInvalidation() {
  const qc = useQueryClient();
  return (opts: { patientId?: string; carePlanId?: string; episodeId?: string }) => {
    if (opts.patientId) qc.invalidateQueries({ queryKey: carePlanKeys.forPatient(opts.patientId) });
    if (opts.carePlanId) qc.invalidateQueries({ queryKey: carePlanKeys.detail(opts.carePlanId) });
    if (opts.episodeId) qc.invalidateQueries({ queryKey: episodeKeys.detail(opts.episodeId) });
    qc.invalidateQueries({ queryKey: ["queue"] });
  };
}

export function useCreateCarePlan() {
  const invalidate = useCarePlanInvalidation();
  return useMutation({
    mutationFn: createCarePlan,
    onSuccess: (plan) => invalidate({ patientId: plan.patientId, episodeId: plan.episodeId }),
  });
}

export function useUpdateCarePlan() {
  const invalidate = useCarePlanInvalidation();
  return useMutation({
    mutationFn: ({ id, status, notes }: { id: string; status?: CarePlanStatus; notes?: string }) =>
      updateCarePlan(id, { status, notes }),
    onSuccess: (plan) => invalidate({ patientId: plan.patientId, carePlanId: plan.id, episodeId: plan.episodeId }),
  });
}

export function useAddFollowUp(carePlanId: string, patientId: string) {
  const invalidate = useCarePlanInvalidation();
  return useMutation({
    mutationFn: (input: { initiatedBy: string; message: string }) => addFollowUp(carePlanId, input),
    onSuccess: () => invalidate({ carePlanId, patientId }),
  });
}

export function useRespondToFollowUp(carePlanId: string, patientId: string) {
  const invalidate = useCarePlanInvalidation();
  return useMutation({
    mutationFn: (input: { followUpId: string; respondedBy: string; response: string }) =>
      respondToFollowUp(carePlanId, input.followUpId, { respondedBy: input.respondedBy, response: input.response }),
    onSuccess: () => invalidate({ carePlanId, patientId }),
  });
}
