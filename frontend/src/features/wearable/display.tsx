import type { MetricTrend, TrendPoint, WearableMetric } from "./types";

// Patient-facing formatting per metric. Values are stored in the metric's canonical unit.
export function formatValue(metric: WearableMetric, value: number | null): string {
  if (value === null || value === undefined) return "—";
  switch (metric) {
    case "SLEEP_MINUTES":
      return `${(value / 60).toFixed(1)} h`;
    case "STEPS":
      return Math.round(value).toLocaleString();
    case "SPO2":
      return `${Math.round(value)}%`;
    default: // HEART_RATE, RESTING_HEART_RATE
      return `${Math.round(value)} bpm`;
  }
}

// A tiny inline sparkline (no charting lib). series is newest-first; we plot it chronologically.
export function Sparkline({ series, width = 120, height = 28 }: { series: TrendPoint[]; width?: number; height?: number }) {
  const points = [...series].reverse();
  if (points.length < 2) return <div style={{ width, height }} className="opacity-40" />;
  const values = points.map((p) => p.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const stepX = width / (points.length - 1);
  const path = points
    .map((p, i) => `${i === 0 ? "M" : "L"} ${(i * stepX).toFixed(1)} ${(height - ((p.value - min) / span) * height).toFixed(1)}`)
    .join(" ");
  return (
    <svg width={width} height={height} className="overflow-visible" aria-hidden="true">
      <path d={path} fill="none" stroke="var(--color-brand-500)" strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}

export function MetricCard({ trend }: { trend: MetricTrend }) {
  return (
    <div className="rounded-2xl border border-line bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-muted">{trend.label}</p>
          <p className="mt-1 text-2xl font-bold tabular-nums text-ink">{formatValue(trend.metric, trend.latest)}</p>
        </div>
        <Sparkline series={trend.series} />
      </div>
      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted">
        <span>avg {formatValue(trend.metric, trend.average)}</span>
        <span>range {formatValue(trend.metric, trend.min)}–{formatValue(trend.metric, trend.max)}</span>
        <span>{trend.count} reading{trend.count === 1 ? "" : "s"}</span>
      </div>
    </div>
  );
}
