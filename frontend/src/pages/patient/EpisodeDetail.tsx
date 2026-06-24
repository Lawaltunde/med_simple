import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Loader2, Sparkles, MessageSquareText, Mic, Camera, type LucideIcon } from "lucide-react";
import { useEpisode } from "@/features/symptom/hooks";
import { effectiveUrgency, type InputType } from "@/features/symptom/types";
import { UrgencyBadge, urgencyMeta, statusLabel, formatDateTime } from "@/features/symptom/display";
import { AuthedMedia } from "@/features/media/AuthedMedia";
import { buttonVariants } from "@/components/ui/button";

const INPUT_ICON: Record<InputType, LucideIcon> = {
  TEXT: MessageSquareText,
  VOICE: Mic,
  PHOTO: Camera,
};

export function EpisodeDetail() {
  const { id } = useParams<{ id: string }>();
  const { data: episode, isLoading, isError } = useEpisode(id);

  return (
    <div className="mx-auto max-w-2xl">
      <Link to="/app" className="mb-5 inline-flex items-center gap-1.5 text-sm font-medium text-muted hover:text-ink">
        <ArrowLeft className="size-4" /> Back to home
      </Link>

      {isLoading && (
        <div className="flex items-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading your symptom check…
        </div>
      )}

      {isError && (
        <div className="rounded-2xl border border-line bg-white p-8 text-center">
          <p className="font-semibold text-ink">We couldn't find that symptom check</p>
          <p className="mt-1 text-sm text-muted">It may have been removed, or the link is wrong.</p>
          <Link to="/app/new" className={buttonVariants({ className: "mt-5" })}>
            Start a new check
          </Link>
        </div>
      )}

      {episode && (
        <div className="space-y-6">
          {/* Urgency verdict — the headline of the whole screen. */}
          <UrgencyPanel episode={episode} />

          {/* AI summary */}
          {episode.aiSummary && (
            <section className="rounded-2xl border border-line bg-white p-6">
              <h3 className="flex items-center gap-2 font-bold text-brand-900">
                <Sparkles className="size-4 text-brand-600" /> What we understood
              </h3>
              <p className="mt-3 whitespace-pre-line text-[0.95rem] leading-relaxed text-ink">{episode.aiSummary}</p>
              {episode.aiAnalyzedAt && (
                <p className="mt-4 text-xs text-muted">Reviewed {formatDateTime(episode.aiAnalyzedAt)}</p>
              )}
            </section>
          )}

          {/* What the patient submitted */}
          <section className="rounded-2xl border border-line bg-white p-6">
            <h3 className="font-bold text-brand-900">What you told us</h3>
            <ul className="mt-4 space-y-4">
              {episode.submissions.map((s) => {
                const Icon = INPUT_ICON[s.inputType];
                return (
                  <li key={s.id} className="flex gap-3">
                    <span className="mt-0.5 inline-flex size-8 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                      <Icon className="size-4" />
                    </span>
                    <div className="min-w-0 space-y-2">
                      {/* The original photo / voice note, fetched with auth. */}
                      {s.mediaUrl && (s.inputType === "PHOTO" || s.inputType === "VOICE") && (
                        <AuthedMedia mediaUrl={s.mediaUrl} inputType={s.inputType} />
                      )}
                      <p className="text-[0.95rem] text-ink">
                        {s.textContent ||
                          s.transcription ||
                          s.aiAnalysisResult ||
                          (s.processingStatus === "PENDING" || s.processingStatus === "PROCESSING"
                            ? s.inputType === "PHOTO"
                              ? "Analyzing your photo…"
                              : "Transcribing your voice note…"
                            : "(media submission)")}
                      </p>
                      <p className="text-xs text-muted">{formatDateTime(s.submittedAt)}</p>
                    </div>
                  </li>
                );
              })}
            </ul>
          </section>

          <p className="text-center text-xs text-muted">
            Status: {statusLabel(episode.status)} · Started {formatDateTime(episode.createdAt)}
          </p>
        </div>
      )}
    </div>
  );
}

function UrgencyPanel({ episode }: { episode: Parameters<typeof effectiveUrgency>[0] }) {
  const urgency = effectiveUrgency(episode);

  if (!urgency) {
    return (
      <section className="rounded-2xl border border-line bg-white p-6">
        <div className="flex items-center gap-2 text-muted">
          <Loader2 className="size-5 animate-spin" />
          <p className="font-medium text-ink">Triage in progress…</p>
        </div>
        <p className="mt-2 text-sm text-muted">We're reviewing your symptoms. This usually takes a moment.</p>
      </section>
    );
  }

  const meta = urgencyMeta(urgency);
  const confirmed = !!episode.confirmedUrgencyLevel;

  return (
    <section
      className="rounded-2xl border-l-4 bg-white p-6 shadow-sm"
      style={{ borderLeftColor: meta.color }}
    >
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-extrabold tracking-tight text-brand-900">Triage result</h2>
        <UrgencyBadge level={urgency} />
      </div>
      <p className="mt-3 text-[0.95rem] leading-relaxed text-ink">{meta.advice}</p>
      <p className="mt-3 text-xs text-muted">
        {confirmed ? "Confirmed by a physician." : "Based on an automated first-pass review. A physician confirms every result."}
      </p>
    </section>
  );
}
