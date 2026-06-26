import { Link } from "react-router-dom";
import { Loader2, ChevronRight, Inbox, Building2 } from "lucide-react";
import { useMyClinics, useQueue } from "@/features/clinician/hooks";
import { queueStats } from "@/features/clinician/display";
import { UrgencyBadge, statusLabel, formatDateTime } from "@/features/symptom/display";

export function QueuePage() {
  const { data: clinics, isLoading: clinicsLoading } = useMyClinics();
  // POC: a physician works one clinic. Multi-clinic switching comes later.
  const clinic = clinics?.[0];
  const { data: queue, isLoading: queueLoading } = useQueue(clinic?.id);
  const stats = queueStats(queue);

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
      <header className="mb-6 border-b border-line pb-5">
        <div className="flex items-baseline justify-between gap-3">
          <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Triage queue</h2>
          <span className="shrink-0 text-sm font-medium text-muted">{clinic.name}</span>
        </div>
        {stats && (
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <StatPill>
              <span className="tabular-nums">{stats.total}</span> waiting
            </StatPill>
            {stats.emergency > 0 && (
              <StatPill color="var(--color-urgency-emergency)">
                <span className="tabular-nums">{stats.emergency}</span> emergency
              </StatPill>
            )}
            {stats.urgent > 0 && (
              <StatPill color="var(--color-urgency-urgent)">
                <span className="tabular-nums">{stats.urgent}</span> urgent
              </StatPill>
            )}
          </div>
        )}
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

// A small queue stat. Neutral by default; pass an urgency color to tint it (a soft fill plus a
// matching dot) so emergency/urgent counts read at a glance without shouting.
function StatPill({ color, children }: { color?: string; children: React.ReactNode }) {
  if (!color) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full border border-line bg-white px-3 py-1 text-sm font-semibold text-ink">
        {children}
      </span>
    );
  }
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-semibold"
      style={{ color, backgroundColor: `color-mix(in srgb, ${color} 12%, white)` }}
    >
      <span className="size-1.5 rounded-full" style={{ backgroundColor: color }} />
      {children}
    </span>
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
