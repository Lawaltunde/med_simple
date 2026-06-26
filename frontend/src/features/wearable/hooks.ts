import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  connectWearable,
  getWearableTrends,
  ingestReadings,
  recordManualReadings,
} from "./api";
import type { IngestReading, WearableTrends } from "./types";

export const wearableKeys = {
  trends: (patientId: string) => ["wearable", "trends", patientId] as const,
};

export function useWearableTrends(patientId: string | undefined) {
  return useQuery({
    queryKey: wearableKeys.trends(patientId ?? ""),
    queryFn: () => getWearableTrends(patientId!),
    enabled: !!patientId,
  });
}

export function useConnectWearable(patientId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => connectWearable(patientId),
    onSuccess: (data: WearableTrends) => qc.setQueryData(wearableKeys.trends(patientId), data),
  });
}

export function useRecordManualReadings(patientId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (readings: IngestReading[]) => recordManualReadings(patientId, readings),
    onSuccess: (data: WearableTrends) => qc.setQueryData(wearableKeys.trends(patientId), data),
  });
}

export function useIngestReadings(patientId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (readings: IngestReading[]) => ingestReadings(patientId, readings),
    onSuccess: (data: WearableTrends) => qc.setQueryData(wearableKeys.trends(patientId), data),
  });
}
