import { Link } from "react-router-dom";
import { PlusCircle, MessageSquareText, Camera, Mic, ArrowRight, Loader2, ChevronRight, ClipboardList } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { buttonVariants } from "@/components/ui/button";
import { useEpisodes } from "@/features/symptom/hooks";
import { effectiveUrgency } from "@/features/symptom/types";
import { UrgencyBadge, statusLabel, formatDateTime } from "@/features/symptom/display";
import { usePatientCarePlans } from "@/features/careplan/hooks";
import { CarePlanStatusBadge } from "@/features/careplan/display";

const RECENT_LIMIT = 5;

export function PatientHome() {
  const { user } = useAuth();
  const { data: episodes, isLoading } = useEpisodes(user?.id);
  const { data: carePlans } = usePatientCarePlans(user?.id);
  const recent = episodes?.slice(0, RECENT_LIMIT) ?? [];
  // Surface plans the physician is still working with the patient on, so a new
  // response is visible the moment polling refreshes the dashboard.
  const activePlans = carePlans?.filter((p) => p.status === "ACTIVE") ?? [];

  return (
    <div className="mx-auto max-w-4xl space-y-8">
      {/* Greeting + primary CTA */}
      <section className="overflow-hidden rounded-2xl bg-gradient-to-br from-brand-600 to-brand-800 p-7 text-white sm:p-9">
        <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
          Hi {user?.firstName} — how are you feeling?
        </h2>
        <p className="mt-2 max-w-md text-brand-100">
          Tell us what's wrong and we'll sort the urgency. A physician reviews every answer.
        </p>
        <Link
          to="/app/new"
          className="mt-6 inline-flex h-12 items-center gap-2 rounded-full bg-white px-6 font-semibold text-brand-700 transition-colors hover:bg-brand-50"
        >
          <PlusCircle className="size-5" /> Start a new symptom check
        </Link>
      </section>

      {/* Active care plans — only shown when a physician has an open plan. */}
      {activePlans.length > 0 && (
        <section>
          <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-muted">From your physician</h3>
          <ul className="space-y-3">
            {activePlans.map((p) => (
              <li key={p.id}>
                <Link
                  to={`/app/care-plans/${p.id}`}
                  className="flex items-center gap-4 rounded-2xl border border-brand-200 bg-brand-50/50 p-4 transition-colors hover:border-brand-300 hover:bg-brand-50"
                >
                  <span className="inline-flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
                    <ClipboardList className="size-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-ink">Care plan</p>
                      <CarePlanStatusBadge status={p.status} />
                    </div>
                    <p className="mt-0.5 truncate text-sm text-muted">{p.notes ?? "Open the plan to read it"}</p>
                  </div>
                  <ChevronRight className="size-4 shrink-0 text-muted" />
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Three ways to ask — each opens the new check in its mode */}
      <section>
        <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-muted">Three ways to ask</h3>
        <div className="grid gap-4 sm:grid-cols-3">
          {[
            { icon: MessageSquareText, title: "Text it", body: "Describe it in your own words.", to: "/app/new" },
            { icon: Camera, title: "Photograph it", body: "Snap a rash, cut, or swelling.", to: "/app/new?mode=PHOTO" },
            { icon: Mic, title: "Speak it", body: "Record a voice note instead.", to: "/app/new?mode=VOICE" },
          ].map((c) => (
            <Link
              key={c.title}
              to={c.to}
              className="group rounded-2xl border border-line bg-white p-5 transition-colors hover:border-brand-300 hover:bg-brand-50/40"
            >
              <span className="inline-flex size-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                <c.icon className="size-5" />
              </span>
              <h4 className="mt-4 font-bold text-brand-900">{c.title}</h4>
              <p className="mt-1 text-sm text-muted">{c.body}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* Recent episodes */}
      <section className="rounded-2xl border border-line bg-white p-6">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-brand-900">Your recent episodes</h3>
          <Link to="/app/new" className={buttonVariants({ variant: "ghost", size: "sm" })}>
            New <ArrowRight className="size-4" />
          </Link>
        </div>

        {isLoading ? (
          <div className="mt-6 flex items-center justify-center gap-2 py-12 text-muted">
            <Loader2 className="size-5 animate-spin" /> Loading…
          </div>
        ) : recent.length === 0 ? (
          <div className="mt-6 flex flex-col items-center justify-center rounded-xl bg-canvas py-12 text-center">
            <p className="font-medium text-ink">No symptom checks yet</p>
            <p className="mt-1 max-w-xs text-sm text-muted">
              When you start a symptom check, it'll appear here with its urgency and status.
            </p>
          </div>
        ) : (
          <ul className="mt-4 divide-y divide-line">
            {recent.map((e) => {
              const urgency = effectiveUrgency(e);
              return (
                <li key={e.id}>
                  <Link
                    to={`/app/episodes/${e.id}`}
                    className="flex items-center gap-3 py-3 transition-colors hover:bg-brand-50/40 -mx-2 rounded-lg px-2"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-[0.95rem] font-medium text-ink">
                        {e.aiSummary ?? e.submissions[0]?.textContent ?? "Symptom check"}
                      </p>
                      <p className="mt-0.5 text-xs text-muted">
                        {statusLabel(e.status)} · {formatDateTime(e.createdAt)}
                      </p>
                    </div>
                    {urgency && <UrgencyBadge level={urgency} />}
                    <ChevronRight className="size-4 shrink-0 text-muted" />
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}
