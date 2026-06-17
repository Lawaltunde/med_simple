import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listMyClinics } from "@/features/clinic/api";
import { episodeKeys } from "@/features/symptom/hooks";
import { assignPhysician, confirmUrgency, getQueue, type ConfirmUrgencyInput } from "./api";

export const clinicianKeys = {
  myClinics: ["me", "clinics"] as const,
  queue: (clinicId: string) => ["queue", clinicId] as const,
};

export function useMyClinics() {
  return useQuery({ queryKey: clinicianKeys.myClinics, queryFn: listMyClinics });
}

export function useQueue(clinicId: string | undefined) {
  return useQuery({
    queryKey: clinicianKeys.queue(clinicId ?? ""),
    queryFn: () => getQueue(clinicId!),
    enabled: !!clinicId,
  });
}

// After a queue action, the queue ordering and the episode detail both change.
function invalidateAfterAction(qc: ReturnType<typeof useQueryClient>, episodeId: string) {
  qc.invalidateQueries({ queryKey: ["queue"] });
  qc.invalidateQueries({ queryKey: episodeKeys.detail(episodeId) });
}

export function useAssignToMe() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ episodeId, physicianId }: { episodeId: string; physicianId: string }) =>
      assignPhysician(episodeId, physicianId),
    onSuccess: (_data, vars) => invalidateAfterAction(qc, vars.episodeId),
  });
}

export function useConfirmUrgency() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ConfirmUrgencyInput) => confirmUrgency(input),
    onSuccess: (_data, vars) => invalidateAfterAction(qc, vars.episodeId),
  });
}
