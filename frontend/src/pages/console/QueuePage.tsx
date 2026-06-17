import { Link } from "react-router-dom";
import { Loader2, ChevronRight, Inbox, Building2 } from "lucide-react";
import { useMyClinics, useQueue } from "@/features/clinician/hooks";
import { UrgencyBadge, statusLabel, formatDateTime } from "@/features/symptom/display";

export function QueuePage() {
  const { data: clinics, isLoading: clinicsLoading } = useMyClinics();
  // POC: a physician works one clinic. Multi-clinic switching comes later.
  const clinic = clinics?.[0];
  const { data: queue, isLoading: queueLoading } = useQueue(clinic?.id);

  if (clinicsLoading) {
    return <Centered><Loader2 className="size-5 animate-spin" /> Loading…</Centered>;
  }

  if (!clinic) {
    return (
      <Empty
        icon={Building2}
        title="You're not attached to a clinic yet"
        body="Ask a clinic admin to add you as staff, then your queue will appear here."
      />
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Triage queue</h2>
        <p className="mt-1 text-muted">
          {clinic.name} · ordered by clinical urgency, most urgent first.
        </p>
      </header>

      {queueLoading ? (
        <Centered><Loader2 className="size-5 animate-spin" /> Loading queue…</Centered>
      ) : !queue || queue.length === 0 ? (
        <Empty icon={Inbox} title="The queue is clear" body="No complaints are waiting for review right now." />
      ) : (
        <ul className="space-y-3">
          {queue.map((entry) => (
            <li key={entry.episodeId}>
              <Link
                to={`/console/episodes/${entry.episodeId}`}
                state={{ patientName: `${entry.patientFirstName} ${entry.patientLastName}` }}
                className="flex items-center gap-4 rounded-2xl border border-line bg-white p-4 transition-colors hover:border-brand-300 hover:bg-brand-50/40"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="truncate font-semibold text-ink">
                      {entry.patientFirstName} {entry.patientLastName}
                    </p>
                    {entry.confirmedUrgencyLevel && (
                      <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-wide text-brand-700">
                        confirmed
                      </span>
                    )}
                  </div>
                  <p className="mt-0.5 truncate text-sm text-muted">{entry.aiSummary ?? "Awaiting triage"}</p>
                  <p className="mt-1 text-xs text-muted">
                    {statusLabel(entry.status)} · {formatDateTime(entry.createdAt)}
                  </p>
                </div>
                {entry.effectiveUrgency && <UrgencyBadge level={entry.effectiveUrgency} />}
                <ChevronRight className="size-4 shrink-0 text-muted" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <div className="flex items-center justify-center gap-2 py-16 text-muted">{children}</div>;
}

function Empty({
  icon: Icon,
  title,
  body,
}: {
  icon: typeof Inbox;
  title: string;
  body: string;
}) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
      <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
        <Icon className="size-6" />
      </span>
      <h2 className="mt-5 text-lg font-bold text-brand-900">{title}</h2>
      <p className="mt-2 text-sm text-muted">{body}</p>
    </div>
  );
}
