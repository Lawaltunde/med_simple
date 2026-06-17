import { Link } from "react-router-dom";
import { Loader2, ClipboardList, ChevronRight } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { usePatientCarePlans } from "@/features/careplan/hooks";
import { CarePlanStatusBadge } from "@/features/careplan/display";
import { formatDateTime } from "@/features/symptom/display";

export function CarePlansPage() {
  const { user } = useAuth();
  const { data: plans, isLoading } = usePatientCarePlans(user?.id);

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Your care plans</h2>
        <p className="mt-1 text-muted">Instructions and messages from the physicians who reviewed your symptoms.</p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !plans || plans.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <ClipboardList className="size-6" />
          </span>
          <p className="mt-5 font-medium text-ink">No care plans yet</p>
          <p className="mt-1 max-w-xs text-sm text-muted">
            When a physician creates a plan for one of your symptom checks, it'll appear here.
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {plans.map((p) => (
            <li key={p.id}>
              <Link
                to={`/app/care-plans/${p.id}`}
                className="flex items-center gap-4 rounded-2xl border border-line bg-white p-4 transition-colors hover:border-brand-300 hover:bg-brand-50/40"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-ink">Care plan</p>
                    <CarePlanStatusBadge status={p.status} />
                  </div>
                  <p className="mt-0.5 truncate text-sm text-muted">{p.notes ?? "No instructions yet"}</p>
                  <p className="mt-1 text-xs text-muted">Updated {formatDateTime(p.updatedAt)}</p>
                </div>
                <ChevronRight className="size-4 shrink-0 text-muted" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
