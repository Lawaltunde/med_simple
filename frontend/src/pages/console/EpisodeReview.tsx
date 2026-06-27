import { useEffect, useState } from "react";
import { Link, useParams, useLocation } from "react-router-dom";
import { ArrowLeft, Loader2, Sparkles, UserCheck, CheckCircle2, AlertCircle, ClipboardList, Bot, X, ImageOff } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { useEpisode } from "@/features/symptom/hooks";
import { effectiveUrgency, type Episode, type UrgencyLevel } from "@/features/symptom/types";
import { UrgencyBadge, urgencyMeta, statusLabel, formatDateTime } from "@/features/symptom/display";
import { useAssignToMe, useConfirmUrgency } from "@/features/clinician/hooks";
import {
  usePatientCarePlans,
  useCreateCarePlan,
  useUpdateCarePlan,
  useEpisodeSuggestion,
  useApproveSuggestion,
  useRejectSuggestion,
} from "@/features/careplan/hooks";
import { CarePlanStatusBadge } from "@/features/careplan/display";
import { FollowUpThread } from "@/features/careplan/FollowUpThread";
import { PrescriptionCard } from "@/features/prescription/PrescriptionCard";
import { DiagnosticCard } from "@/features/diagnostic/DiagnosticCard";
import { ReferralCard } from "@/features/referral/ReferralCard";
import { WearableSummaryCard } from "@/features/wearable/WearableSummaryCard";
import { BookAppointmentCard } from "@/features/appointment/BookAppointmentCard";
import { AuthedMedia } from "@/features/media/AuthedMedia";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";

const URGENCY_ORDER: UrgencyLevel[] = ["ROUTINE", "SEE_SOON", "URGENT", "EMERGENCY"];

export function EpisodeReview() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const patientName = (location.state as { patientName?: string } | null)?.patientName;
  const { data: episode, isLoading, isError } = useEpisode(id);

  return (
    <div className="mx-auto max-w-2xl">
      <Link to="/console" className="mb-5 inline-flex items-center gap-1.5 text-sm font-medium text-muted hover:text-ink">
        <ArrowLeft className="size-4" /> Back to queue
      </Link>

      {isLoading && (
        <div className="flex items-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading episode…
        </div>
      )}

      {isError && (
        <div className="rounded-2xl border border-line bg-white p-8 text-center">
          <p className="font-semibold text-ink">Couldn't load this episode</p>
          <Link to="/console" className="mt-4 inline-block text-sm font-semibold text-brand-600 hover:underline">
            Back to queue
          </Link>
        </div>
      )}

      {episode && id && (
        <div className="space-y-6">
          <header>
            <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">{patientName ?? "Patient"}</h2>
            <p className="mt-1 text-sm text-muted">
              {statusLabel(episode.status)} · started {formatDateTime(episode.createdAt)}
            </p>
          </header>

          {/* AI assessment */}
          <section className="rounded-2xl border border-line bg-white p-6">
            <div className="flex items-center justify-between">
              <h3 className="flex items-center gap-2 font-bold text-brand-900">
                <Sparkles className="size-4 text-brand-600" /> AI assessment
              </h3>
              {episode.aiUrgencyLevel && <UrgencyBadge level={episode.aiUrgencyLevel} />}
            </div>
            {episode.aiSummary && (
              <p className="mt-3 whitespace-pre-line text-[0.95rem] leading-relaxed text-ink">{episode.aiSummary}</p>
            )}
          </section>

          {/* Patient submissions */}
          <section className="rounded-2xl border border-line bg-white p-6">
            <h3 className="font-bold text-brand-900">What the patient reported</h3>
            <ul className="mt-3 space-y-3">
              {episode.submissions.map((s) => (
                <li key={s.id} className="space-y-2 text-[0.95rem] text-ink">
                  <div>
                    {s.textContent || s.transcription || s.aiAnalysisResult || "(media submission)"}
                    <span className="ml-2 text-xs text-muted">{formatDateTime(s.submittedAt)}</span>
                  </div>
                  {/* Flag photos the AI couldn't confirm a symptom in, so the reading below isn't
                      mistaken for a confident clinical finding. */}
                  {s.inputType === "PHOTO" &&
                    (s.aiAnalysisRelevance === "NO_SYMPTOM" || s.aiAnalysisRelevance === "UNSURE") && (
                      <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-2.5 text-xs text-amber-800">
                        <ImageOff className="mt-0.5 size-3.5 shrink-0 text-amber-600" />
                        <span>
                          {s.aiAnalysisRelevance === "NO_SYMPTOM"
                            ? "No visible symptom detected in this image — the patient chose to send it anyway. Treat the reading above as unconfirmed."
                            : "The AI couldn't confirm a symptom in this image. Treat the reading above as unconfirmed."}
                        </span>
                      </div>
                    )}
                  {/* The actual photo / voice note, not just the AI's text reading of it. The GET is
                      ownership-gated and staff-allowed, so AuthedMedia fetches it with the token. */}
                  {s.mediaUrl && (s.inputType === "PHOTO" || s.inputType === "VOICE") && (
                    <AuthedMedia mediaUrl={s.mediaUrl} inputType={s.inputType} />
                  )}
                </li>
              ))}
            </ul>
          </section>

          <AssignCard episode={episode} episodeId={id} />
          <WearableSummaryCard patientId={episode.patientId} />
          <ConfirmUrgencyCard episode={episode} episodeId={id} />
          <CarePlanCard episode={episode} episodeId={id} />
          {/* Workstream slots — each renders null until its branch lands. */}
          <PrescriptionCard episode={episode} episodeId={id} />
          <DiagnosticCard episode={episode} episodeId={id} />
          <ReferralCard episode={episode} episodeId={id} />
          <BookAppointmentCard episode={episode} episodeId={id} />
        </div>
      )}
    </div>
  );
}

function CarePlanCard({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const { data: plans, isLoading } = usePatientCarePlans(episode.patientId);
  const update = useUpdateCarePlan();

  const plan = plans?.find((p) => p.episodeId === episodeId);

  if (isLoading) {
    return (
      <section className="rounded-2xl border border-line bg-white p-6">
        <div className="flex items-center gap-2 text-muted"><Loader2 className="size-4 animate-spin" /> Loading care plan…</div>
      </section>
    );
  }

  // No plan yet — author one (from the AI suggestion if there is one). Creating it moves
  // the episode to IN_PROGRESS.
  if (!plan) {
    return <PlanAuthoring episode={episode} episodeId={episodeId} />;
  }

  // Plan exists — show it, allow completion (resolves the episode), and message the patient.
  return (
    <section className="space-y-5 rounded-2xl border border-line bg-white p-6">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 font-bold text-brand-900">
          <ClipboardList className="size-4 text-brand-600" /> Care plan
        </h3>
        <CarePlanStatusBadge status={plan.status} />
      </div>
      <p className="whitespace-pre-line text-[0.95rem] leading-relaxed text-ink">{plan.notes}</p>

      {plan.status === "ACTIVE" && (
        <Button
          variant="outline"
          disabled={update.isPending}
          onClick={() => update.mutate({ id: plan.id, status: "COMPLETED" })}
        >
          {update.isPending ? <Loader2 className="size-4 animate-spin" /> : <CheckCircle2 className="size-4" />}
          Mark complete (resolves & clears the queue)
        </Button>
      )}

      <div className="border-t border-line pt-5">
        {user && <FollowUpThread plan={plan} currentUserId={user.id} />}
      </div>
    </section>
  );
}

// Authoring a new plan: if the AI proposed one, the physician edits/approves/rejects it;
// otherwise they write from scratch. Approving (or edit-then-approving) is the only path
// that turns an AI suggestion into a patient-visible plan.
function PlanAuthoring({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const { data: suggestion } = useEpisodeSuggestion(episodeId);
  const create = useCreateCarePlan();
  const approve = useApproveSuggestion(episodeId, episode.patientId);
  const reject = useRejectSuggestion(episodeId);
  const [notes, setNotes] = useState("");
  const [rejected, setRejected] = useState(false);

  const aiPlan = suggestion?.status === "PROPOSED" ? suggestion.suggestedPlan ?? null : null;
  const showAi = !!aiPlan && !rejected;

  // Pre-fill the editor with the AI draft once it loads (physician edits in place).
  useEffect(() => {
    if (aiPlan && !rejected) setNotes(aiPlan);
  }, [aiPlan, rejected]);

  const tooShort = notes.trim().length < 5;
  const pending = create.isPending || approve.isPending || reject.isPending;

  function submit() {
    if (!user || tooShort) return;
    if (showAi && suggestion) {
      const edited = notes.trim() !== (suggestion.suggestedPlan ?? "").trim();
      approve.mutate({ id: suggestion.id, physicianId: user.id, editedNotes: edited ? notes.trim() : undefined });
    } else {
      create.mutate({ episodeId, physicianId: user.id, notes: notes.trim() });
    }
  }

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <h3 className="flex items-center gap-2 font-bold text-brand-900">
        {showAi ? <Bot className="size-4 text-brand-600" /> : <ClipboardList className="size-4 text-brand-600" />}
        {showAi ? "AI suggested care plan" : "Create care plan"}
      </h3>

      {showAi ? (
        <div className="mt-3 rounded-xl border border-brand-200 bg-brand-50/60 p-3 text-xs text-brand-800">
          AI-generated suggestion. Advisory only — you are the responsible clinician. Review, edit, or
          reject before it reaches the patient.
          {suggestion?.rationale && <p className="mt-1 font-medium text-brand-700">Why: {suggestion.rationale}</p>}
        </div>
      ) : (
        <p className="mt-1 text-sm text-muted">Write the plan and instructions. The patient is notified and can reply.</p>
      )}

      <Textarea
        name="notes"
        className="mt-4"
        rows={5}
        placeholder="e.g. Likely viral pharyngitis. Rest, fluids, acetaminophen as needed. Return if fever > 39°C or symptoms persist past 5 days."
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
        disabled={pending}
      />

      <div className="mt-4 flex flex-wrap gap-2">
        <Button disabled={pending || tooShort || !user} onClick={submit}>
          {pending ? <Loader2 className="size-4 animate-spin" /> : <CheckCircle2 className="size-4" />}
          {showAi ? "Approve & send to patient" : "Create care plan"}
        </Button>
        {showAi && suggestion && (
          <Button
            variant="ghost"
            disabled={pending || !user}
            onClick={() =>
              user &&
              reject.mutate(
                { id: suggestion.id, physicianId: user.id },
                { onSuccess: () => { setRejected(true); setNotes(""); } }
              )
            }
          >
            <X className="size-4" /> Reject, write my own
          </Button>
        )}
      </div>
    </section>
  );
}

function AssignCard({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const assign = useAssignToMe();
  const mineAlready = episode.assignedPhysicianId === user?.id;
  const assignedToOther = !!episode.assignedPhysicianId && !mineAlready;

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <h3 className="font-bold text-brand-900">Ownership</h3>
      {mineAlready ? (
        <p className="mt-2 flex items-center gap-2 text-sm font-medium text-brand-700">
          <CheckCircle2 className="size-4" /> Assigned to you
        </p>
      ) : (
        <>
          <p className="mt-2 text-sm text-muted">
            {assignedToOther
              ? "Currently assigned to another physician. Assigning will take it over."
              : "Take ownership of this complaint to start working it."}
          </p>
          <Button
            className="mt-4"
            variant={assignedToOther ? "outline" : "primary"}
            disabled={assign.isPending || !user}
            onClick={() => user && assign.mutate({ episodeId, physicianId: user.id })}
          >
            {assign.isPending ? <Loader2 className="size-4 animate-spin" /> : <UserCheck className="size-4" />}
            {assignedToOther ? "Take over" : "Assign to me"}
          </Button>
          {assign.error && (
            <p className="mt-3 text-sm text-[--color-urgency-emergency]" role="alert">
              {assign.error instanceof ApiError ? assign.error.message : "Could not assign this episode."}
            </p>
          )}
        </>
      )}
    </section>
  );
}

function ConfirmUrgencyCard({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const confirm = useConfirmUrgency();
  const [level, setLevel] = useState<UrgencyLevel>(effectiveUrgency(episode) ?? "ROUTINE");
  const [reason, setReason] = useState("");

  const isOverride = episode.aiUrgencyLevel != null && level !== episode.aiUrgencyLevel;
  const reasonMissing = isOverride && reason.trim().length === 0;
  const alreadyConfirmed = episode.confirmedUrgencyLevel === level;

  function submit() {
    if (!user || reasonMissing) return;
    confirm.mutate({
      episodeId,
      physicianId: user.id,
      confirmedUrgencyLevel: level,
      overrideReason: isOverride ? reason.trim() : undefined,
    });
  }

  const errorMessage =
    confirm.error instanceof ApiError ? confirm.error.message : confirm.error ? "Couldn't save. Try again." : null;

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <h3 className="font-bold text-brand-900">Confirm urgency</h3>
      <p className="mt-1 text-sm text-muted">
        Agree with the AI or override it. Overriding records your reason on the episode.
      </p>

      <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
        {URGENCY_ORDER.map((lvl) => {
          const meta = urgencyMeta(lvl);
          const selected = level === lvl;
          return (
            <button
              key={lvl}
              type="button"
              onClick={() => setLevel(lvl)}
              aria-pressed={selected}
              className="flex items-center justify-center rounded-xl border-2 px-2 py-2 text-xs font-bold uppercase tracking-wide transition-colors"
              style={{
                borderColor: selected ? meta.color : "var(--color-line)",
                backgroundColor: selected ? meta.color : "transparent",
                color: selected ? "white" : "var(--color-muted)",
              }}
            >
              {meta.label}
            </button>
          );
        })}
      </div>

      {isOverride && (
        <div className="mt-4">
          <Textarea
            name="overrideReason"
            label="Reason for override"
            placeholder="Why does this differ from the AI assessment?"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
            error={reasonMissing ? "A reason is required when you override the AI." : undefined}
          />
        </div>
      )}

      {errorMessage && (
        <div role="alert" className="mt-4 flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
          <AlertCircle className="mt-0.5 size-4 shrink-0" /> {errorMessage}
        </div>
      )}

      <Button
        className="mt-5"
        disabled={confirm.isPending || reasonMissing || alreadyConfirmed}
        onClick={submit}
      >
        {confirm.isPending ? <Loader2 className="size-4 animate-spin" /> : null}
        {alreadyConfirmed ? "Confirmed" : isOverride ? "Override urgency" : "Confirm urgency"}
      </Button>
    </section>
  );
}
