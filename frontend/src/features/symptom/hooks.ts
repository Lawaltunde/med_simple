import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getEpisode, listEpisodes, submitTextSymptomCheck, type TextSymptomCheck } from "./api";
import type { Episode } from "./types";

export const episodeKeys = {
  all: ["episodes"] as const,
  list: (patientId: string) => ["episodes", "list", patientId] as const,
  detail: (episodeId: string) => ["episodes", "detail", episodeId] as const,
};

// Poll so triage results and physician status changes surface without a manual refresh.
const POLL_MS = 30_000;

export function useEpisodes(patientId: string | undefined) {
  return useQuery({
    queryKey: episodeKeys.list(patientId ?? ""),
    queryFn: () => listEpisodes(patientId!),
    enabled: !!patientId,
    refetchInterval: POLL_MS,
  });
}

export function useEpisode(episodeId: string | undefined) {
  return useQuery({
    queryKey: episodeKeys.detail(episodeId ?? ""),
    queryFn: () => getEpisode(episodeId!),
    enabled: !!episodeId,
    refetchInterval: POLL_MS,
  });
}

export function useSubmitSymptomCheck() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: TextSymptomCheck) => submitTextSymptomCheck(input),
    onSuccess: (episode: Episode) => {
      // Seed the detail cache so the result screen renders instantly, and refresh the list.
      qc.setQueryData(episodeKeys.detail(episode.id), episode);
      qc.invalidateQueries({ queryKey: episodeKeys.list(episode.patientId) });
    },
  });
}
