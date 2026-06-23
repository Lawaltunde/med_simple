import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Loader2, ClipboardList } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { useCarePlan } from "@/features/careplan/hooks";
import { CarePlanStatusBadge } from "@/features/careplan/display";
import { FollowUpThread } from "@/features/careplan/FollowUpThread";
import { formatDateTime } from "@/features/symptom/display";

export function CarePlanDetail() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const { data: plan, isLoading, isError } = useCarePlan(id);

  return (
    <div className="mx-auto max-w-2xl">
      <Link to="/app/care-plans" className="mb-5 inline-flex items-center gap-1.5 text-sm font-medium text-muted hover:text-ink">
        <ArrowLeft className="size-4" /> Back to care plans
      </Link>

      {isLoading && (
        <div className="flex items-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      )}

      {isError && (
        <div className="rounded-2xl border border-line bg-white p-8 text-center">
          <p className="font-semibold text-ink">Couldn't load this care plan</p>
        </div>
      )}

      {plan && user && (
        <div className="space-y-6">
          <section className="rounded-2xl border border-line bg-white p-6">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-extrabold tracking-tight text-brand-900">
                <ClipboardList className="size-5 text-brand-600" /> Care plan
              </h2>
              <CarePlanStatusBadge status={plan.status} />
            </div>
            <p className="mt-4 whitespace-pre-line text-[0.95rem] leading-relaxed text-ink">
              {plan.notes ?? "Your physician hasn't added written instructions yet."}
            </p>
            <p className="mt-4 text-xs text-muted">Created {formatDateTime(plan.createdAt)}</p>
          </section>

          <section className="rounded-2xl border border-line bg-white p-6">
            <FollowUpThread plan={plan} currentUserId={user.id} />
          </section>
        </div>
      )}
    </div>
  );
}
