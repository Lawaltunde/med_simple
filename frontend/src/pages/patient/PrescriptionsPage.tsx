import { Pill, Loader2, ChevronRight } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { formatDateTime } from "@/features/symptom/display";
import { usePatientPrescriptions } from "@/features/prescription/hooks";
import { PrescriptionStatusBadge, deliveryLabel } from "@/features/prescription/display";

export function PrescriptionsPage() {
  const { user } = useAuth();
  const { data: prescriptions, isLoading } = usePatientPrescriptions(user?.id);

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Your prescriptions</h2>
        <p className="mt-1 text-muted">Medications prescribed by your physician and their delivery status.</p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !prescriptions || prescriptions.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Pill className="size-6" />
          </span>
          <p className="mt-5 font-medium text-ink">No prescriptions yet</p>
          <p className="mt-1 max-w-xs text-sm text-muted">
            When a physician prescribes medication it will appear here along with the pharmacy delivery status.
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {prescriptions.map((rx) => (
            <li key={rx.id}>
              <div className="flex items-center gap-4 rounded-2xl border border-line bg-white p-4">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold text-ink">{rx.medicationName}</p>
                    <PrescriptionStatusBadge status={rx.status} />
                  </div>
                  <p className="mt-0.5 text-sm text-muted">
                    {rx.dosage} · {rx.frequency}
                    {rx.duration ? ` · ${rx.duration}` : ""}
                    {rx.refills > 0 ? ` · ${rx.refills} refill${rx.refills !== 1 ? "s" : ""}` : ""}
                  </p>
                  {rx.instructions && (
                    <p className="mt-0.5 text-xs text-muted">Note: {rx.instructions}</p>
                  )}
                  <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-muted">
                    <span>Delivery: {deliveryLabel(rx.deliveryMethod)}</span>
                    <span>Prescribed {formatDateTime(rx.createdAt)}</span>
                    {rx.filledAt && <span>Filled {formatDateTime(rx.filledAt)}</span>}
                  </div>
                </div>

                {rx.documentUrl ? (
                  <a
                    href={rx.documentUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`View PDF for ${rx.medicationName}`}
                    className="shrink-0 text-brand-600 hover:text-brand-800"
                  >
                    <ChevronRight className="size-4" />
                  </a>
                ) : (
                  <ChevronRight className="size-4 shrink-0 text-muted" />
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
