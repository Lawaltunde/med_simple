import { api } from "@/lib/api";
import type {
  Requisition,
  PhysicianResult,
  PatientResult,
  RequisitionUrgency,
} from "./types";

// ── Requisitions ─────────────────────────────────────────────────────────────

// Requisitions hang off the episode now (a physician can order a work-up without a care plan).
export function getEpisodeRequisitions(episodeId: string): Promise<Requisition[]> {
  return api<Requisition[]>(`/episodes/${episodeId}/requisitions`);
}

export function createRequisition(input: {
  episodeId: string;
  testType: string;
  testCodes?: string[];
  clinicalIndication?: string;
  urgency: RequisitionUrgency;
}): Promise<Requisition> {
  return api<Requisition>("/requisitions", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function sendRequisition(id: string): Promise<Requisition> {
  return api<Requisition>(`/requisitions/${id}/send`, { method: "POST" });
}

export function cancelRequisition(id: string): Promise<Requisition> {
  return api<Requisition>(`/requisitions/${id}/cancel`, { method: "POST" });
}

// ── Results: physician side ───────────────────────────────────────────────────

export function recordResult(
  requisitionId: string,
  input: { rawResultText: string; rawResultData?: string }
): Promise<PhysicianResult> {
  return api<PhysicianResult>(`/requisitions/${requisitionId}/result`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function addInterpretation(
  resultId: string,
  input: { physicianInterpretation: string }
): Promise<PhysicianResult> {
  return api<PhysicianResult>(`/results/${resultId}/interpretation`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function releaseResult(resultId: string): Promise<PhysicianResult> {
  return api<PhysicianResult>(`/results/${resultId}/release`, { method: "POST" });
}

export function getResultForPhysician(resultId: string): Promise<PhysicianResult> {
  return api<PhysicianResult>(`/results/${resultId}`);
}

// ── Results: patient side (released-only, gate enforced server-side) ─────────

export function listPatientResults(patientId: string): Promise<PatientResult[]> {
  return api<PatientResult[]>(`/patients/${patientId}/results`);
}
