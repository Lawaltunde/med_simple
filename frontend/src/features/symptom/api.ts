import { api } from "@/lib/api";
import { uploadMedia, filenameFor } from "@/features/media/api";
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

// Run the media analysis (transcription / vision) synchronously so the result is on the
// submission before triage reads it. Returns the processed submission.
function processSubmission(submissionId: string): Promise<Submission> {
  return api<Submission>(`/submissions/${submissionId}/process`, { method: "POST" });
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

export interface MediaSymptomCheck {
  patientId: string;
  inputType: Extract<InputType, "PHOTO" | "VOICE">;
  blob: Blob;
  clinicId?: string;
}

/**
 * The full photo/voice-intake flow as one operation:
 *   1. upload the blob → in-platform mediaUrl
 *   2. create the episode
 *   3. attach the media as a submission (PENDING)
 *   4. process it synchronously → transcription (voice) / analysis (photo) lands COMPLETED
 *   5. run AI triage on the resulting text
 *
 * Steps are sequential by necessity — each needs the id from the previous one. Processing is
 * driven explicitly here rather than waiting on the background poller, so the patient sees a
 * triaged result in one flow.
 */
export async function submitMediaSymptomCheck(input: MediaSymptomCheck): Promise<Episode> {
  const contentType = input.blob.type || (input.inputType === "PHOTO" ? "image/jpeg" : "audio/webm");
  const { mediaUrl } = await uploadMedia(input.blob, filenameFor(contentType));

  const episode = await startEpisode(input.patientId, input.clinicId);
  const submission = await addSubmission(episode.id, { inputType: input.inputType, mediaUrl });
  await processSubmission(submission.id);
  return triageEpisode(episode.id);
}
