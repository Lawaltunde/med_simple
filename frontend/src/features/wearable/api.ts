import { api } from "@/lib/api";
import type { IngestReading, WearableTrends } from "./types";

export function getWearableTrends(patientId: string): Promise<WearableTrends> {
  return api<WearableTrends>(`/patients/${patientId}/wearable/trends`);
}

// Sync from the connected device source (synthetic in the fallback). Requires WEARABLE_DATA consent.
export function connectWearable(patientId: string): Promise<WearableTrends> {
  return api<WearableTrends>(`/patients/${patientId}/wearable/connect`, { method: "POST" });
}

// Aggregator / webhook-style batch ingest (also the curl-testable path).
export function ingestReadings(patientId: string, readings: IngestReading[]): Promise<WearableTrends> {
  return api<WearableTrends>(`/patients/${patientId}/wearable/ingest`, {
    method: "POST",
    body: JSON.stringify({ readings }),
  });
}

// Manual entry by the patient or staff.
export function recordManualReadings(patientId: string, readings: IngestReading[]): Promise<WearableTrends> {
  return api<WearableTrends>(`/patients/${patientId}/wearable/readings`, {
    method: "POST",
    body: JSON.stringify({ readings }),
  });
}
