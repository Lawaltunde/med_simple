import { Activity } from "lucide-react";
import { useWearableTrends } from "./hooks";
import { formatValue } from "./display";

/**
 * Compact, read-only wearable summary for the physician console. Hidden when the patient has shared
 * no data. Always labeled consumer-grade — the physician interprets; the platform never concludes.
 */
export function WearableSummaryCard({ patientId }: { patientId: string }) {
  const { data } = useWearableTrends(patientId);
  if (!data || data.metrics.length === 0) return null;

  return (
    <div className="rounded-2xl border border-line bg-white p-5">
      <div className="flex flex-wrap items-center gap-2">
        <Activity className="size-4 text-brand-600" />
        <h3 className="font-semibold text-ink">Wearable vitals</h3>
        <span className="rounded-full bg-amber-50 px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-wide text-amber-700">
          consumer-grade
        </span>
      </div>
      <p className="mt-2 text-sm text-muted">{data.summary}</p>
      <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1.5 text-sm">
        {data.metrics.map((m) => (
          <span key={m.metric}>
            <span className="text-muted">{m.label}: </span>
            <span className="font-semibold tabular-nums text-ink">{formatValue(m.metric, m.latest)}</span>
          </span>
        ))}
      </div>
    </div>
  );
}
