import { api } from "@/lib/api";
import type { Episode, InputType, Submission } from "./types";

// ── Single endpoints ──────────────────────────────────────────────────────────

export function listEpisodes(patientId: string): Promise<Episode[]> {
  return api<Episode[]>(`/patients/${patientId}/episodes`);
}

export function getEpisode(episodeId: string): Promise<Episode> {
  return api<Episode>(`/episodes/${episodeId}`);
}

function startEpisode(patientId: string, clinicId?: string): Promise<Episode> {
  return api<Episode>(`/patients/${patientId}/episodes`, {
    method: "POST",
    body: JSON.stringify({ clinicId: clinicId ?? null }),
  });
}

function addSubmission(
  episodeId: string,
  body: { inputType: InputType; textContent?: string; mediaUrl?: string }
): Promise<Submission> {
  return api<Submission>(`/episodes/${episodeId}/submissions`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

function triageEpisode(episodeId: string): Promise<Episode> {
  return api<Episode>(`/episodes/${episodeId}/triage`, { method: "POST" });
}

// ── Composed patient flow ──────────────────────────────────────────────────────

export interface TextSymptomCheck {
  patientId: string;
  description: string;
  clinicId?: string;
}

/**
 * The full text-intake flow as one operation: create the episode, attach the typed
 * description as a submission, then run AI triage. Resolves to the triaged episode
 * (status AI_ANALYZED, with urgency + summary populated).
 *
 * Steps are sequential by necessity — each needs the id from the previous one.
 */
export async function submitTextSymptomCheck(input: TextSymptomCheck): Promise<Episode> {
  const episode = await startEpisode(input.patientId, input.clinicId);
  await addSubmission(episode.id, { inputType: "TEXT", textContent: input.description });
  return triageEpisode(episode.id);
}
