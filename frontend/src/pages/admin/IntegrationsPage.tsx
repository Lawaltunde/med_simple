import { Loader2 } from "lucide-react";
import { useMyClinics, useCapabilities, useSetCapability } from "@/features/clinic/hooks";
import { CAPABILITY_META } from "@/features/clinic/display";
import type { CapabilityType } from "@/features/clinic/api";

const ALL: CapabilityType[] = [
  "PRESCRIBIT", "OCEAN_EREFERRAL", "EMR_FHIR", "LAB_ELECTRONIC", "LAB_BOOKING", "PHARMACY_ORDERING",
];

export function IntegrationsPage() {
  const { data: clinics } = useMyClinics();
  const clinicId = clinics?.[0]?.id;
  const { data: capabilities, isLoading } = useCapabilities(clinicId);
  const set = useSetCapability(clinicId ?? "");

  const enabledOf = (c: CapabilityType) => capabilities?.find((x) => x.capability === c)?.enabled ?? false;

  return (
    <div className="mx-auto max-w-3xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Integrations</h2>
        <p className="mt-1 text-muted">
          Turn on a rail when your clinic is connected. While it is off, MedSimple uses the built-in
          fallback, so every workflow still works.
        </p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : (
        <ul className="space-y-3">
          {ALL.map((c) => {
            const meta = CAPABILITY_META[c];
            const enabled = enabledOf(c);
            return (
              <li key={c} className="flex items-start gap-4 rounded-2xl border border-line bg-white p-4">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-ink">{meta.label}</p>
                  <p className="mt-0.5 text-sm text-muted">{meta.description}</p>
                  {!enabled && (
                    <p className="mt-1 text-xs text-muted">Fallback in use: {meta.fallback}</p>
                  )}
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={enabled}
                  aria-label={meta.label}
                  disabled={set.isPending || !clinicId}
                  onClick={() => set.mutate({ capability: c, enabled: !enabled })}
                  className={`relative mt-1 inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors disabled:opacity-50 ${
                    enabled ? "bg-brand-600" : "bg-line"
                  }`}
                >
                  <span
                    className={`inline-block size-5 transform rounded-full bg-white shadow transition-transform ${
                      enabled ? "translate-x-5" : "translate-x-0.5"
                    }`}
                  />
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
