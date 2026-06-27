import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getEpisode,
  listEpisodes,
  submitTextSymptomCheck,
  submitMediaSymptomCheck,
  triageEpisode,
  type TextSymptomCheck,
  type MediaSymptomCheck,
  type MediaSubmitResult,
} from "./api";
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

function seedEpisode(qc: ReturnType<typeof useQueryClient>, episode: Episode) {
  // Seed the detail cache so the result screen renders instantly, and refresh the list.
  qc.setQueryData(episodeKeys.detail(episode.id), episode);
  qc.invalidateQueries({ queryKey: episodeKeys.list(episode.patientId) });
}

export function useSubmitSymptomCheck() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: TextSymptomCheck) => submitTextSymptomCheck(input),
    onSuccess: (episode: Episode) => seedEpisode(qc, episode),
  });
}

// Photo/voice intake: upload → submission → synchronous process → triage, as one mutation.
// A photo the AI saw no symptom in returns { kind: "needs_review" } instead, untriaged.
export function useSubmitMediaSymptomCheck() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: MediaSymptomCheck) => submitMediaSymptomCheck(input),
    onSuccess: (result: MediaSubmitResult) => {
      if (result.kind === "triaged") seedEpisode(qc, result.episode);
    },
  });
}

// "Send for review anyway": triage the photo episode the patient chose to forward.
export function useTriageEpisode() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (episodeId: string) => triageEpisode(episodeId),
    onSuccess: (episode: Episode) => seedEpisode(qc, episode),
  });
}
