import { Loader2, Send } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { formatDateTime } from "@/features/symptom/display";
import { usePatientReferrals } from "@/features/referral/hooks";
import { ReferralStatusBadge, ReferralUrgencyBadge, deliveryLabel } from "@/features/referral/display";

export function ReferralsPage() {
  const { user } = useAuth();
  const { data: referrals, isLoading } = usePatientReferrals(user?.id);

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Your referrals</h2>
        <p className="mt-1 text-muted">
          Specialist referrals your physician has made on your behalf. Status updates automatically.
        </p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !referrals || referrals.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Send className="size-6" />
          </span>
          <p className="mt-5 font-medium text-ink">No referrals yet</p>
          <p className="mt-1 max-w-xs text-sm text-muted">
            When a physician refers you to a specialist, it'll appear here with its current status.
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {referrals.map((r) => (
            <li
              key={r.id}
              className="rounded-2xl border border-line bg-white p-5"
            >
              {/* Top row: specialty + badges */}
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-ink">
                  {r.confirmedSpecialty ?? r.aiSuggestedSpecialty}
                </p>
                <ReferralStatusBadge status={r.status} />
                <ReferralUrgencyBadge urgency={r.urgency} />
              </div>

              {/* Clinical summary — truncated */}
              <p className="mt-2 line-clamp-2 text-sm text-muted">{r.clinicalSummary}</p>

              {/* Delivery method + dates */}
              <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-muted">
                {r.deliveryMethod && (
                  <span>{deliveryLabel(r.deliveryMethod)}</span>
                )}
                {r.submittedAt ? (
                  <span>Submitted {formatDateTime(r.submittedAt)}</span>
                ) : (
                  <span>Created {formatDateTime(r.createdAt)}</span>
                )}
              </div>

              {/* PDF link when available */}
              {r.documentUrl && (
                <a
                  href={r.documentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-brand-600 hover:underline"
                >
                  View referral letter
                </a>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
