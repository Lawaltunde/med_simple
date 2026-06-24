import { useState } from "react";
import {
  Loader2,
  FlaskConical,
  Send,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  XCircle,
} from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import type { Episode } from "@/features/symptom/types";
import type { Requisition, PhysicianResult, RequisitionUrgency } from "./types";
import { RequisitionStatusBadge, UrgencyBadge } from "./display";
import { formatDateTime } from "@/features/symptom/display";
import {
  useEpisodeRequisitions,
  useCreateRequisition,
  useSendRequisition,
  useCancelRequisition,
  useRecordResult,
  useAddInterpretation,
  useReleaseResult,
} from "./hooks";

// ── Helper: show the result sub-panel for a resulted requisition ──────────────
// The result id is not carried on the Requisition DTO. After a physician records
// a result, we hold the returned PhysicianResult in local state.

function ResultPanel({
  requisition,
  episodeId,
  patientId,
}: {
  requisition: Requisition;
  episodeId: string;
  patientId: string;
}) {
  // Result ID is not on the requisition DTO — track locally after recording.
  const [localResult, setLocalResult] = useState<PhysicianResult | null>(null);
  const [rawText, setRawText] = useState("");
  const [interpretation, setInterpretation] = useState("");

  const recordResult = useRecordResult();
  const addInterpretation = useAddInterpretation();
  const releaseResult = useReleaseResult();

  const activeResult = localResult;

  const recordError =
    recordResult.error instanceof ApiError
      ? recordResult.error.message
      : recordResult.error
      ? "Failed to record result"
      : null;

  const releaseError =
    releaseResult.error instanceof ApiError
      ? releaseResult.error.message
      : releaseResult.error
      ? "Failed to release result"
      : null;

  // No result recorded yet — show the recording form.
  if (!activeResult) {
    return (
      <div className="mt-4 space-y-3 border-t border-line pt-4">
        <p className="text-sm font-medium text-ink">Record lab result</p>
        <Textarea
          name="rawResultText"
          label="Raw result text"
          placeholder="Paste or type the result from the lab (e.g. WBC: 7.2, HGB: 138…)"
          rows={4}
          value={rawText}
          onChange={(e) => setRawText(e.target.value)}
          disabled={recordResult.isPending}
        />
        {recordError && (
          <div role="alert" className="flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
            <AlertCircle className="mt-0.5 size-4 shrink-0" /> {recordError}
          </div>
        )}
        <Button
          size="sm"
          disabled={recordResult.isPending || rawText.trim().length < 3}
          onClick={() =>
            recordResult.mutate(
              { requisitionId: requisition.id, episodeId, rawResultText: rawText.trim() },
              { onSuccess: (r) => setLocalResult(r) }
            )
          }
        >
          {recordResult.isPending ? <Loader2 className="size-4 animate-spin" /> : <CheckCircle2 className="size-4" />}
          Record result
        </Button>
      </div>
    );
  }

  // Result exists — show AI summary, interpretation box, release button.
  return (
    <div className="mt-4 space-y-4 border-t border-line pt-4">
      {/* Raw result (physician-only) */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-muted">Raw result</p>
        <p className="mt-1 whitespace-pre-line text-sm text-ink">{activeResult.rawResultText}</p>
      </div>

      {/* AI summary */}
      {activeResult.aiSummary && (
        <div className="rounded-xl bg-brand-50 p-4">
          <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-brand-700">
            <Sparkles className="size-3.5" /> AI summary
          </p>
          <p className="mt-1.5 text-sm text-ink">{activeResult.aiSummary}</p>
        </div>
      )}

      {/* Physician interpretation */}
      {activeResult.releasedToPatient ? (
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">Your interpretation</p>
          <p className="mt-1 text-sm text-ink">
            {activeResult.physicianInterpretation ?? <span className="italic text-muted">None recorded</span>}
          </p>
          <div className="mt-2 flex items-center gap-1.5 text-xs font-medium text-green-700">
            <CheckCircle2 className="size-3.5" />
            Released to patient {formatDateTime(activeResult.releasedToPatientAt!)}
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <Textarea
            name="physicianInterpretation"
            label="Physician interpretation (shown to patient on release)"
            placeholder="e.g. All values within normal limits. No action required at this time."
            rows={3}
            value={interpretation}
            onChange={(e) => setInterpretation(e.target.value)}
            disabled={addInterpretation.isPending || releaseResult.isPending}
          />
          <div className="flex flex-wrap items-center gap-2">
            {interpretation.trim().length >= 3 && (
              <Button
                size="sm"
                variant="outline"
                disabled={addInterpretation.isPending}
                onClick={() =>
                  addInterpretation.mutate(
                    { resultId: activeResult.id, episodeId, physicianInterpretation: interpretation.trim() },
                    { onSuccess: (r) => setLocalResult(r) }
                  )
                }
              >
                {addInterpretation.isPending ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <CheckCircle2 className="size-4" />
                )}
                Save interpretation
              </Button>
            )}
            <Button
              size="sm"
              disabled={releaseResult.isPending}
              onClick={() =>
                releaseResult.mutate(
                  { resultId: activeResult.id, episodeId, patientId },
                  { onSuccess: (r) => setLocalResult(r) }
                )
              }
            >
              {releaseResult.isPending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
              Release to patient
            </Button>
          </div>
          {releaseError && (
            <div role="alert" className="flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
              <AlertCircle className="mt-0.5 size-4 shrink-0" /> {releaseError}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Single requisition row ────────────────────────────────────────────────────

function RequisitionRow({
  req,
  episodeId,
  patientId,
}: {
  req: Requisition;
  episodeId: string;
  patientId: string;
}) {
  const send = useSendRequisition();
  const cancel = useCancelRequisition();
  const [expanded, setExpanded] = useState(false);

  const canSend = req.status === "DRAFT";
  const canCancel = req.status === "DRAFT" || req.status === "SENT";
  const showResultPanel =
    req.status === "SENT" ||
    req.status === "BOOKED" ||
    req.status === "IN_PROGRESS" ||
    req.status === "RESULTED";

  return (
    <li className="rounded-xl border border-line bg-canvas">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="flex w-full items-center justify-between gap-3 rounded-xl px-4 py-3 text-left transition-colors hover:bg-brand-50/40"
        aria-expanded={expanded}
      >
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold text-ink">{req.testType}</span>
            <RequisitionStatusBadge status={req.status} />
            <UrgencyBadge urgency={req.urgency} />
          </div>
          {req.clinicalIndication && (
            <p className="mt-0.5 truncate text-xs text-muted">{req.clinicalIndication}</p>
          )}
        </div>
        <span className="text-xs text-muted">{expanded ? "▲" : "▼"}</span>
      </button>

      {expanded && (
        <div className="border-t border-line px-4 pb-4">
          <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
            <div>
              <dt className="font-semibold uppercase tracking-wide text-muted">Ordered</dt>
              <dd className="mt-0.5 text-ink">{formatDateTime(req.createdAt)}</dd>
            </div>
            {req.sentAt && (
              <div>
                <dt className="font-semibold uppercase tracking-wide text-muted">Sent</dt>
                <dd className="mt-0.5 text-ink">{formatDateTime(req.sentAt)}</dd>
              </div>
            )}
            {req.testCodes && req.testCodes.length > 0 && (
              <div className="col-span-2">
                <dt className="font-semibold uppercase tracking-wide text-muted">Codes</dt>
                <dd className="mt-0.5 text-ink">{req.testCodes.join(", ")}</dd>
              </div>
            )}
          </dl>

          {/* Actions */}
          <div className="mt-3 flex flex-wrap gap-2">
            {canSend && (
              <Button
                size="sm"
                disabled={send.isPending}
                onClick={(e) => {
                  e.stopPropagation();
                  send.mutate({ id: req.id, episodeId });
                }}
              >
                {send.isPending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
                Send to lab
              </Button>
            )}
            {canCancel && (
              <Button
                size="sm"
                variant="outline"
                disabled={cancel.isPending}
                onClick={(e) => {
                  e.stopPropagation();
                  cancel.mutate({ id: req.id, episodeId });
                }}
              >
                {cancel.isPending ? <Loader2 className="size-4 animate-spin" /> : <XCircle className="size-4" />}
                Cancel
              </Button>
            )}
          </div>

          {/* Result sub-panel — available once the lab has the requisition */}
          {showResultPanel && (
            <ResultPanel requisition={req} episodeId={episodeId} patientId={patientId} />
          )}
        </div>
      )}
    </li>
  );
}

// ── Create requisition form ───────────────────────────────────────────────────

const URGENCY_OPTIONS: { value: RequisitionUrgency; label: string }[] = [
  { value: "ROUTINE", label: "Routine" },
  { value: "URGENT", label: "Urgent" },
  { value: "STAT", label: "STAT" },
];

function CreateRequisitionForm({ episodeId }: { episodeId: string }) {
  const [testType, setTestType] = useState("");
  const [clinicalIndication, setClinicalIndication] = useState("");
  const [urgency, setUrgency] = useState<RequisitionUrgency>("ROUTINE");
  const [open, setOpen] = useState(false);

  const create = useCreateRequisition();

  const createError =
    create.error instanceof ApiError
      ? create.error.message
      : create.error
      ? "Failed to create requisition"
      : null;

  function submit() {
    if (testType.trim().length < 2) return;
    create.mutate(
      { episodeId, testType: testType.trim(), clinicalIndication: clinicalIndication.trim() || undefined, urgency },
      {
        onSuccess: () => {
          setTestType("");
          setClinicalIndication("");
          setUrgency("ROUTINE");
          setOpen(false);
        },
      }
    );
  }

  if (!open) {
    return (
      <Button size="sm" variant="outline" onClick={() => setOpen(true)}>
        <FlaskConical className="size-4" /> Order new test
      </Button>
    );
  }

  return (
    <div className="space-y-3 rounded-xl border border-line bg-canvas p-4">
      <p className="text-sm font-semibold text-ink">New diagnostic requisition</p>
      <div>
        <label htmlFor="testType" className="mb-1 block text-xs font-medium text-ink">
          Test type <span className="text-[--color-urgency-emergency]">*</span>
        </label>
        <input
          id="testType"
          className="w-full rounded-lg border border-line bg-white px-3 py-2 text-sm text-ink focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25"
          placeholder="e.g. Complete Blood Count, Chest X-Ray"
          value={testType}
          onChange={(e) => setTestType(e.target.value)}
          disabled={create.isPending}
        />
      </div>
      <Textarea
        name="clinicalIndication"
        label="Clinical indication"
        placeholder="Reason for ordering this test"
        rows={2}
        value={clinicalIndication}
        onChange={(e) => setClinicalIndication(e.target.value)}
        disabled={create.isPending}
      />
      <div>
        <p className="mb-1.5 text-xs font-medium text-ink">Urgency</p>
        <div className="flex gap-2">
          {URGENCY_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              aria-pressed={urgency === opt.value}
              disabled={create.isPending}
              onClick={() => setUrgency(opt.value)}
              className="flex-1 rounded-lg border-2 py-1.5 text-xs font-bold uppercase tracking-wide transition-colors"
              style={{
                borderColor: urgency === opt.value ? "var(--color-brand-600)" : "var(--color-line)",
                backgroundColor: urgency === opt.value ? "var(--color-brand-600)" : "transparent",
                color: urgency === opt.value ? "white" : "var(--color-muted)",
              }}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>
      {createError && (
        <div role="alert" className="flex items-start gap-2 text-sm text-[--color-urgency-emergency]">
          <AlertCircle className="mt-0.5 size-4 shrink-0" /> {createError}
        </div>
      )}
      <div className="flex gap-2">
        <Button size="sm" disabled={create.isPending || testType.trim().length < 2} onClick={submit}>
          {create.isPending ? <Loader2 className="size-4 animate-spin" /> : <FlaskConical className="size-4" />}
          Create requisition
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setOpen(false)} disabled={create.isPending}>
          Cancel
        </Button>
      </div>
    </div>
  );
}

// ── Main DiagnosticCard ───────────────────────────────────────────────────────

export function DiagnosticCard({ episode }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();

  // Physician-only. Patients see their released results on ResultsPage instead.
  if (!user || (user.role !== "PHYSICIAN" && user.role !== "NURSE" && user.role !== "CLINIC_ADMIN")) {
    return null;
  }

  return <DiagnosticCardInner episode={episode} />;
}

// Requisitions hang off the episode, so a physician can order a work-up without a care plan.
function DiagnosticCardInner({ episode }: { episode: Episode }) {
  const episodeId = episode.id;

  const { data: requisitions, isLoading } = useEpisodeRequisitions(episodeId);

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <h3 className="flex items-center gap-2 font-bold text-brand-900">
        <FlaskConical className="size-4 text-brand-600" /> Diagnostics
      </h3>
      <p className="mt-1 text-sm text-muted">Order tests, record results, and release them to the patient.</p>

      {isLoading ? (
        <div className="mt-4 flex items-center gap-2 text-sm text-muted">
          <Loader2 className="size-4 animate-spin" /> Loading requisitions…
        </div>
      ) : requisitions && requisitions.length > 0 ? (
        <ul className="mt-4 space-y-2">
          {requisitions.map((req) => (
            <RequisitionRow
              key={req.id}
              req={req}
              episodeId={episodeId}
              patientId={episode.patientId}
            />
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-sm text-muted">No tests ordered yet.</p>
      )}

      <div className="mt-4">
        <CreateRequisitionForm episodeId={episodeId} />
      </div>
    </section>
  );
}
