// Mirrors the backend wearable DTOs.

export type WearableMetric =
  | "HEART_RATE"
  | "RESTING_HEART_RATE"
  | "STEPS"
  | "SLEEP_MINUTES"
  | "SPO2";

export interface TrendPoint {
  at: string; // ISO-8601
  value: number;
}

export interface MetricTrend {
  metric: WearableMetric;
  label: string;
  unit: string;
  latest: number | null;
  latestAt: string | null;
  average: number | null;
  min: number | null;
  max: number | null;
  count: number;
  series: TrendPoint[];
}

export interface WearableTrends {
  connected: boolean;
  metrics: MetricTrend[];
  summary: string;
}

export interface IngestReading {
  metric: WearableMetric;
  value: number;
  effectiveAt?: string;
}
