import { Loader2, Activity, Watch, ShieldCheck } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { useConsents, useSetConsent, isConsentActive } from "@/features/consent/hooks";
import { useWearableTrends, useConnectWearable } from "@/features/wearable/hooks";
import { MetricCard } from "@/features/wearable/display";

export function VitalsPage() {
  const { user } = useAuth();
  const patientId = user?.id;

  const { data: consents } = useConsents(patientId);
  const consented = isConsentActive(consents, "WEARABLE_DATA");
  const setConsent = useSetConsent(patientId ?? "");
  const connect = useConnectWearable(patientId ?? "");
  const { data: trends, isLoading } = useWearableTrends(patientId);

  const busy = setConsent.isPending || connect.isPending;

  async function handleConnect() {
    if (!consented) await setConsent.mutateAsync({ consentType: "WEARABLE_DATA", grant: true });
    await connect.mutateAsync();
  }

  const hasData = !!trends && trends.metrics.length > 0;

  return (
    <div className="mx-auto max-w-3xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Vitals &amp; activity</h2>
        <p className="mt-1 text-muted">
          Heart rate, steps, sleep, and blood oxygen from your watch or phone, shared with your clinic.
        </p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !hasData ? (
        <div className="mx-auto flex max-w-md flex-col items-center rounded-2xl border border-line bg-white px-6 py-14 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Watch className="size-6" />
          </span>
          <h3 className="mt-5 text-lg font-bold text-brand-900">Connect a device</h3>
          <p className="mt-2 text-sm text-muted">
            Share your watch or phone health data with your care team. You can disconnect anytime in Privacy &amp; AI.
          </p>
          <button
            type="button"
            onClick={handleConnect}
            disabled={busy || !patientId}
            className="mt-5 inline-flex items-center gap-2 rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-700 disabled:opacity-50"
          >
            {busy ? <Loader2 className="size-4 animate-spin" /> : <Activity className="size-4" />}
            {consented ? "Connect device" : "Consent & connect"}
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            {trends!.metrics.map((m) => (
              <MetricCard key={m.metric} trend={m} />
            ))}
          </div>
          <p className="flex items-start gap-2 rounded-xl bg-brand-50/60 px-4 py-3 text-xs text-muted">
            <ShieldCheck className="mt-0.5 size-4 shrink-0 text-brand-600" />
            Patient-generated, consumer-grade data. Shown to give your clinician context. It is not a
            medical-device measurement, and your clinician interprets it.
          </p>
          <button
            type="button"
            onClick={handleConnect}
            disabled={busy}
            className="text-sm font-medium text-brand-700 hover:text-brand-900 disabled:opacity-50"
          >
            {busy ? "Syncing…" : "Sync latest"}
          </button>
        </div>
      )}
    </div>
  );
}
