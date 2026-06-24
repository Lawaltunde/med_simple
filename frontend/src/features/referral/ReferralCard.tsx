import { useState } from "react";
import { Loader2, Send, Sparkles, CheckCircle2, AlertCircle, ExternalLink, Bot } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import type { Episode } from "@/features/symptom/types";
import { formatDateTime } from "@/features/symptom/display";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import { useEpisodeSuggestion } from "@/features/careplan/hooks";
import {
  useEpisodeReferrals,
  useConfirmSpecialty,
  useCreateReferral,
  useSubmitReferral,
} from "./hooks";
import { ReferralStatusBadge, ReferralUrgencyBadge, deliveryLabel } from "./display";
import type { Referral, ReferralUrgency } from "./types";

// Physician-only slot rendered inside EpisodeReview. Referrals hang off the episode, so a
// physician can refer without first writing a care plan.
export function ReferralCard({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  if (user?.role !== "PHYSICIAN") return null;
  return <ReferralCardInner episodeId={episodeId || episode.id} />;
}

function ReferralCardInner({ episodeId }: { episodeId: string }) {
  const { data: referrals, isLoading } = useEpisodeReferrals(episodeId);

  if (isLoading) {
    return (
      <section className="rounded-2xl border border-line bg-white p-6">
        <div className="flex items-center gap-2 text-muted">
          <Loader2 className="size-4 animate-spin" /> Loading referrals…
        </div>
      </section>
    );
  }

  return (
    <section className="space-y-4 rounded-2xl border border-line bg-white p-6">
      <h3 className="flex items-center gap-2 font-bold text-brand-900">
        <Send className="size-4 text-brand-600" /> Specialist referral
      </h3>

      {referrals && referrals.length > 0 ? (
        <div className="space-y-4">
          {referrals.map((r) => (
            <ExistingReferral key={r.id} referral={r} episodeId={episodeId} />
          ))}
        </div>
      ) : (
        <CreateReferralForm episodeId={episodeId} />
      )}
    </section>
  );
}

// ── Create form ────────────────────────────────────────────────────────────────

const URGENCY_OPTIONS: { value: ReferralUrgency; label: string }[] = [
  { value: "ROUTINE", label: "Routine" },
  { value: "URGENT", label: "Urgent" },
  { value: "EMERGENT", label: "Emergent" },
];

function CreateReferralForm({ episodeId }: { episodeId: string }) {
  const [summary, setSummary] = useState("");
  const [urgency, setUrgency] = useState<ReferralUrgency>("ROUTINE");
  const create = useCreateReferral(episodeId);
  const { data: suggestion } = useEpisodeSuggestion(episodeId);

  const aiSpecialty = suggestion?.suggestedReferralSpecialty ?? null;

  const tooShort = summary.trim().length < 10;
  const errorMessage =
    create.error instanceof ApiError
      ? create.error.message
      : create.error
        ? "Couldn't create referral. Try again."
        : null;

  // Prefill the clinical summary with a referral-to-{specialty} stem. The physician edits it and
  // submits via the existing create flow, which runs its own backend specialty suggester.
  function useAiSpecialty() {
    if (!aiSpecialty) return;
    setSummary((prev) =>
      prev.trim().length > 0 ? prev : `Referral to ${aiSpecialty} for further assessment of `
    );
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted">
        The AI will suggest a specialty based on the clinical summary. You can confirm or override
        it before the referral is sent.
      </p>

      {aiSpecialty && (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-200 bg-brand-50/60 p-3 text-sm text-brand-800">
          <span className="flex items-center gap-1.5">
            <Bot className="size-4 text-brand-600" />
            AI suggests referral to {aiSpecialty}.
          </span>
          <Button size="sm" variant="outline" onClick={useAiSpecialty} disabled={create.isPending}>
            Use this
          </Button>
        </div>
      )}

      <Textarea
        name="clinicalSummary"
        label="Clinical summary"
        placeholder="e.g. 52-year-old with progressive exertional dyspnoea and bilateral lower limb oedema. Query cardiac aetiology."
        rows={4}
        value={summary}
        onChange={(e) => setSummary(e.target.value)}
        disabled={create.isPending}
      />

      <fieldset>
        <legend className="mb-2 text-sm font-medium text-ink">Urgency</legend>
        <div className="flex gap-2">
          {URGENCY_OPTIONS.map(({ value, label }) => {
            const selected = urgency === value;
            return (
              <button
                key={value}
                type="button"
                onClick={() => setUrgency(value)}
                aria-pressed={selected}
                className="rounded-lg border-2 px-3 py-1.5 text-xs font-bold uppercase tracking-wide transition-colors"
                style={{
                  borderColor: selected ? "var(--color-brand-600)" : "var(--color-line)",
                  backgroundColor: selected ? "var(--color-brand-600)" : "transparent",
                  color: selected ? "white" : "var(--color-muted)",
                }}
              >
                {label}
              </button>
            );
          })}
        </div>
      </fieldset>

      {errorMessage && (
        <div role="alert" className="flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
          <AlertCircle className="mt-0.5 size-4 shrink-0" /> {errorMessage}
        </div>
      )}

      <Button
        disabled={create.isPending || tooShort}
        onClick={() => create.mutate({ clinicalSummary: summary.trim(), urgency })}
      >
        {create.isPending ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
        {create.isPending ? "Getting AI suggestion…" : "Create referral"}
      </Button>
    </div>
  );
}

// ── Existing referral ──────────────────────────────────────────────────────────

function ExistingReferral({ referral, episodeId }: { referral: Referral; episodeId: string }) {
  const confirm = useConfirmSpecialty(episodeId);
  const submit = useSubmitReferral(episodeId);

  const [specialtyInput, setSpecialtyInput] = useState(
    referral.confirmedSpecialty ?? referral.aiSuggestedSpecialty
  );
  const isDraft = referral.status === "DRAFT";
  const specialtyConfirmedAndUnchanged =
    !!referral.confirmedSpecialty && specialtyInput.trim() === referral.confirmedSpecialty;

  const confirmError =
    confirm.error instanceof ApiError
      ? confirm.error.message
      : confirm.error
        ? "Couldn't confirm specialty."
        : null;
  const submitError =
    submit.error instanceof ApiError
      ? submit.error.message
      : submit.error
        ? "Couldn't submit referral."
        : null;

  return (
    <div className="space-y-4 rounded-xl border border-line p-4">
      {/* Header row */}
      <div className="flex flex-wrap items-center gap-2">
        <ReferralStatusBadge status={referral.status} />
        <ReferralUrgencyBadge urgency={referral.urgency} />
        {referral.deliveryMethod && (
          <span className="text-xs text-muted">{deliveryLabel(referral.deliveryMethod)}</span>
        )}
        <span className="ml-auto text-xs text-muted">{formatDateTime(referral.createdAt)}</span>
      </div>

      {/* AI suggestion */}
      <div className="flex items-center gap-2 text-sm">
        <Sparkles className="size-3.5 shrink-0 text-brand-500" />
        <span className="text-muted">AI suggestion:</span>
        <span className="font-semibold text-ink">{referral.aiSuggestedSpecialty}</span>
      </div>

      {/* Confirm / override specialty — only on DRAFT */}
      {isDraft ? (
        <div className="flex items-center gap-2">
          <input
            type="text"
            aria-label="Specialty"
            className="h-9 flex-1 rounded-lg border border-line bg-white px-3 text-sm text-ink focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-200 disabled:opacity-50"
            value={specialtyInput}
            onChange={(e) => setSpecialtyInput(e.target.value)}
            disabled={confirm.isPending}
            placeholder="Accept or type a different specialty"
          />
          <Button
            variant="outline"
            disabled={confirm.isPending || !specialtyInput.trim()}
            onClick={() =>
              confirm.mutate({ id: referral.id, confirmedSpecialty: specialtyInput.trim() })
            }
          >
            {confirm.isPending ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <CheckCircle2 className={`size-4 ${specialtyConfirmedAndUnchanged ? "text-brand-600" : ""}`} />
            )}
            {specialtyConfirmedAndUnchanged ? "Confirmed" : "Confirm specialty"}
          </Button>
        </div>
      ) : (
        referral.confirmedSpecialty && (
          <div className="flex items-center gap-2 text-sm">
            <CheckCircle2 className="size-3.5 shrink-0 text-brand-600" />
            <span className="text-muted">Confirmed specialty:</span>
            <span className="font-semibold text-ink">{referral.confirmedSpecialty}</span>
          </div>
        )
      )}

      {/* Clinical summary */}
      <p className="rounded-lg bg-canvas px-3 py-2.5 text-sm text-ink">{referral.clinicalSummary}</p>

      {/* Post-submission details */}
      {referral.submittedAt && (
        <p className="text-xs text-muted">Submitted {formatDateTime(referral.submittedAt)}</p>
      )}

      {referral.documentUrl && (
        <a
          href={referral.documentUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-600 hover:underline"
        >
          <ExternalLink className="size-3.5" /> View referral letter
        </a>
      )}

      {/* Error alerts */}
      {confirmError && (
        <div role="alert" className="flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
          <AlertCircle className="mt-0.5 size-4 shrink-0" /> {confirmError}
        </div>
      )}
      {submitError && (
        <div role="alert" className="flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
          <AlertCircle className="mt-0.5 size-4 shrink-0" /> {submitError}
        </div>
      )}

      {/* Submit — only on DRAFT, requires confirmed specialty */}
      {isDraft && (
        <Button
          disabled={submit.isPending || !referral.confirmedSpecialty}
          title={!referral.confirmedSpecialty ? "Confirm the specialty first" : undefined}
          onClick={() => submit.mutate(referral.id)}
        >
          {submit.isPending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
          {submit.isPending ? "Sending referral…" : "Submit referral"}
        </Button>
      )}
    </div>
  );
}
