import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addInterpretation,
  cancelRequisition,
  createRequisition,
  getCarePlanRequisitions,
  getResultForPhysician,
  listPatientResults,
  recordResult,
  releaseResult,
  sendRequisition,
} from "./api";
import type { RequisitionUrgency } from "./types";

export const diagnosticKeys = {
  forCarePlan: (carePlanId: string) => ["diagnostics", "requisitions", carePlanId] as const,
  result: (resultId: string) => ["diagnostics", "result", resultId] as const,
  patientResults: (patientId: string) => ["diagnostics", "patient-results", patientId] as const,
};

const POLL_MS = 30_000;

// ── Read ──────────────────────────────────────────────────────────────────────

export function useCarePlanRequisitions(carePlanId: string | undefined) {
  return useQuery({
    queryKey: diagnosticKeys.forCarePlan(carePlanId ?? ""),
    queryFn: () => getCarePlanRequisitions(carePlanId!),
    enabled: !!carePlanId,
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
  return (opts: { carePlanId?: string; resultId?: string; patientId?: string }) => {
    if (opts.carePlanId) {
      qc.invalidateQueries({ queryKey: diagnosticKeys.forCarePlan(opts.carePlanId) });
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
      carePlanId: string;
      testType: string;
      testCodes?: string[];
      clinicalIndication?: string;
      urgency: RequisitionUrgency;
    }) => createRequisition(input),
    onSuccess: (req) => invalidate({ carePlanId: req.carePlanId }),
  });
}

export function useSendRequisition() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({ id }: { id: string; carePlanId: string }) =>
      sendRequisition(id),
    onSuccess: (_req, vars) => invalidate({ carePlanId: vars.carePlanId }),
  });
}

export function useCancelRequisition() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({ id }: { id: string; carePlanId: string }) =>
      cancelRequisition(id),
    onSuccess: (_req, vars) => invalidate({ carePlanId: vars.carePlanId }),
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
      carePlanId: string;
      rawResultText: string;
      rawResultData?: string;
    }) => recordResult(requisitionId, { rawResultText, rawResultData }),
    onSuccess: (result, vars) =>
      invalidate({ carePlanId: vars.carePlanId, resultId: result.id }),
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
      carePlanId: string;
      physicianInterpretation: string;
    }) => addInterpretation(resultId, { physicianInterpretation }),
    onSuccess: (result, vars) =>
      invalidate({ carePlanId: vars.carePlanId, resultId: result.id }),
  });
}

export function useReleaseResult() {
  const invalidate = useDiagnosticInvalidation();
  return useMutation({
    mutationFn: ({ resultId }: {
      resultId: string;
      carePlanId: string;
      patientId: string;
    }) => releaseResult(resultId),
    onSuccess: (result, vars) =>
      invalidate({
        carePlanId: vars.carePlanId,
        resultId: result.id,
        patientId: vars.patientId,
      }),
  });
}
