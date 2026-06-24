import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addInterpretation,
  cancelRequisition,
  createRequisition,
  getEpisodeRequisitions,
  getResultForPhysician,
  listPatientResults,
  recordResult,
  releaseResult,
  sendRequisition,
} from "./api";
import type { RequisitionUrgency } from "./types";

export const diagnosticKeys = {
  forEpisode: (episodeId: string) => ["diagnostics", "requisitions", episodeId] as const,
  result: (resultId: string) => ["diagnostics", "result", resultId] as const,
  patientResults: (patientId: string) => ["diagnostics", "patient-results", patientId] as const,
};

const POLL_MS = 30_000;

// ── Read ──────────────────────────────────────────────────────────────────────

export function useEpisodeRequisitions(episodeId: string | undefined) {
  return useQuery({
    queryKey: diagnosticKeys.forEpisode(episodeId ?? ""),
    queryFn: () => getEpisodeRequisitions(episodeId!),
    enabled: !!episodeId,
    refetchInterval: POLL_MS,
  });
}

export function usePhysicianResult(resultId: string | undefined) {
  return useQuery({
    queryKey: diagnosticKeys.result(resultId ?? ""),
    queryFn: () => getResultForPhysician(resultId!),
    enabled: !!resultId,
    refetchInterval: POLL_MS,
  });
}

// Patient endpoint: returns released results only — gate is server-side.
export function usePatientResults(patientId: string | undefined) {
  return useQuery({
    queryKey: diagnosticKeys.patientResults(patientId ?? ""),
    queryFn: () => listPatientResults(patientId!),
    enabled: !!patientId,
    refetchInterval: POLL_MS,
  });
}

// ── Mutations ─────────────────────────────────────────────────────────────────

function useDiagnosticInvalidation() {
  const qc = useQueryClient();
  return (opts: { episodeId?: string; resultId?: string; patientId?: string }) => {
    if (opts.episodeId) {
      qc.invalidateQueries({ queryKey: diagnosticKeys.forEpisode(opts.episodeId) });
    }
    if (opts.resultId) {
      qc.invalidateQueries({ queryKey: diagnosticKeys.result(opts.resultId) });
    }
    if (opts.patientId) {
      qc.invalidateQueries({ queryKey: diagnosticKeys.patientResults(opts.patientId) });
    }
  };
}

export function useCreateRequisition() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: (input: {
      episodeId: string;
      testType: string;
      testCodes?: string[];
      clinicalIndication?: string;
      urgency: RequisitionUrgency;
    }) => createRequisition(input),
    onSuccess: (req) => invalidate({ episodeId: req.episodeId }),
  });
}

export function useSendRequisition() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({ id }: { id: string; episodeId: string }) => sendRequisition(id),
    onSuccess: (_req, vars) => invalidate({ episodeId: vars.episodeId }),
  });
}

export function useCancelRequisition() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({ id }: { id: string; episodeId: string }) => cancelRequisition(id),
    onSuccess: (_req, vars) => invalidate({ episodeId: vars.episodeId }),
  });
}

export function useRecordResult() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({
      requisitionId,
      rawResultText,
      rawResultData,
    }: {
      requisitionId: string;
      episodeId: string;
      rawResultText: string;
      rawResultData?: string;
    }) => recordResult(requisitionId, { rawResultText, rawResultData }),
    onSuccess: (result, vars) => invalidate({ episodeId: vars.episodeId, resultId: result.id }),
  });
}

export function useAddInterpretation() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({
      resultId,
      physicianInterpretation,
    }: {
      resultId: string;
      episodeId: string;
      physicianInterpretation: string;
    }) => addInterpretation(resultId, { physicianInterpretation }),
    onSuccess: (result, vars) => invalidate({ episodeId: vars.episodeId, resultId: result.id }),
  });
}

export function useReleaseResult() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({ resultId }: { resultId: string; episodeId: string; patientId: string }) =>
      releaseResult(resultId),
    onSuccess: (result, vars) =>
      invalidate({ episodeId: vars.episodeId, resultId: result.id, patientId: vars.patientId }),
  });
}
