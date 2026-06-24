import { useState } from "react";
import { Pill, Loader2, AlertTriangle, CheckCircle2, Send, PenLine, XCircle } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import type { Episode } from "@/features/symptom/types";
import { formatDateTime } from "@/features/symptom/display";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import {
  useEpisodePrescriptions,
  useCreatePrescription,
  useSignPrescription,
  useSendPrescription,
  useCancelPrescription,
} from "./hooks";
import { PrescriptionStatusBadge, deliveryLabel } from "./display";
import type { AiSafetyResult, Prescription } from "./types";

// ── Helpers ──────────────────────────────────────────────────────────────────

function parseAiSafety(raw: string | null): AiSafetyResult | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AiSafetyResult;
  } catch {
    return null;
  }
}

function hasAiWarnings(s: AiSafetyResult | null): boolean {
  if (!s) return false;
  return s.interactions.length > 0 || s.allergyFlags.length > 0 || s.doseWarnings.length > 0;
}

// ── Safety result panel ──────────────────────────────────────────────────────

function AiSafetyPanel({ raw }: { raw: string | null }) {
  const safety = parseAiSafety(raw);
  if (!safety) return null;
  const warn = hasAiWarnings(safety);

  return (
    <div
      className={`mt-4 rounded-xl border p-4 text-sm ${
        warn
          ? "border-amber-300 bg-amber-50 text-amber-900"
          : "border-line bg-canvas text-muted"
      }`}
    >
      <p className="flex items-center gap-1.5 font-semibold">
        {warn ? (
          <AlertTriangle className="size-4 text-amber-600 shrink-0" />
        ) : (
          <CheckCircle2 className="size-4 text-brand-600 shrink-0" />
        )}
        AI medication safety
      </p>

      {!warn && <p className="mt-1">No interactions, allergy flags, or dose warnings detected.</p>}

      {safety.allergyFlags.length > 0 && (
        <div className="mt-2">
          <p className="font-medium">Allergy flags</p>
          <ul className="mt-0.5 list-disc pl-4">
            {safety.allergyFlags.map((f, i) => <li key={i}>{f}</li>)}
          </ul>
        </div>
      )}

      {safety.interactions.length > 0 && (
        <div className="mt-2">
          <p className="font-medium">Interactions</p>
          <ul className="mt-0.5 list-disc pl-4">
            {safety.interactions.map((f, i) => <li key={i}>{f}</li>)}
          </ul>
        </div>
      )}

      {safety.doseWarnings.length > 0 && (
        <div className="mt-2">
          <p className="font-medium">Dose warnings</p>
          <ul className="mt-0.5 list-disc pl-4">
            {safety.doseWarnings.map((f, i) => <li key={i}>{f}</li>)}
          </ul>
        </div>
      )}
    </div>
  );
}

// ── Prescription row ─────────────────────────────────────────────────────────

function PrescriptionRow({
  rx,
  onSign,
  onSend,
  onCancel,
  busy,
}: {
  rx: Prescription;
  onSign: (id: string) => void;
  onSend: (id: string) => void;
  onCancel: (id: string) => void;
  busy: boolean;
}) {
  const safety = parseAiSafety(rx.aiSafetyChecks);
  const warn = hasAiWarnings(safety);

  return (
    <li className="rounded-xl border border-line bg-canvas p-4">
      <div className="flex flex-wrap items-start gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-semibold text-ink">{rx.medicationName}</p>
            <PrescriptionStatusBadge status={rx.status} />
            {warn && (
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-800">
                <AlertTriangle className="size-3" /> AI warning
              </span>
            )}
          </div>
          <p className="mt-0.5 text-sm text-muted">
            {rx.dosage} · {rx.frequency}
            {rx.duration ? ` · ${rx.duration}` : ""}
            {rx.refills > 0 ? ` · ${rx.refills} refill${rx.refills !== 1 ? "s" : ""}` : ""}
          </p>
          {rx.instructions && (
            <p className="mt-1 text-xs text-muted">Note: {rx.instructions}</p>
          )}
          {rx.deliveryMethod && (
            <p className="mt-1 text-xs text-muted">Delivery: {deliveryLabel(rx.deliveryMethod)}</p>
          )}
          <p className="mt-1 text-xs text-muted">Created {formatDateTime(rx.createdAt)}</p>
        </div>

        {/* State-machine actions */}
        <div className="flex flex-col gap-1.5">
          {rx.status === "DRAFT" && (
            <>
              <Button
                size="sm"
                variant="outline"
                disabled={busy}
                onClick={() => onSign(rx.id)}
              >
                {busy ? <Loader2 className="size-3 animate-spin" /> : <PenLine className="size-3" />}
                Sign
              </Button>
              <Button
                size="sm"
                variant="ghost"
                disabled={busy}
                onClick={() => onCancel(rx.id)}
                className="text-[--color-urgency-emergency]"
              >
                <XCircle className="size-3" /> Cancel
              </Button>
            </>
          )}
          {rx.status === "SIGNED" && (
            <>
              <Button
                size="sm"
                disabled={busy}
                onClick={() => onSend(rx.id)}
              >
                {busy ? <Loader2 className="size-3 animate-spin" /> : <Send className="size-3" />}
                Send
              </Button>
              <Button
                size="sm"
                variant="ghost"
                disabled={busy}
                onClick={() => onCancel(rx.id)}
                className="text-[--color-urgency-emergency]"
              >
                <XCircle className="size-3" /> Cancel
              </Button>
            </>
          )}
          {rx.documentUrl && (
            <a
              href={rx.documentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-medium text-brand-600 hover:underline"
            >
              View PDF
            </a>
          )}
        </div>
      </div>

      {/* AI safety result surfaced on the card */}
      {safety && <AiSafetyPanel raw={rx.aiSafetyChecks} />}
    </li>
  );
}

// ── New prescription form ────────────────────────────────────────────────────

const FIELD_CLASS =
  "w-full rounded-lg border border-line bg-white px-3 py-2 text-sm text-ink placeholder-muted focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-200 disabled:opacity-50";
const LABEL_CLASS = "block text-xs font-semibold text-muted mb-1";

interface FormState {
  medicationName: string;
  medicationCode: string;
  dosage: string;
  frequency: string;
  duration: string;
  refills: string;
  instructions: string;
}

const EMPTY: FormState = {
  medicationName: "",
  medicationCode: "",
  dosage: "",
  frequency: "",
  duration: "",
  refills: "0",
  instructions: "",
};

function NewPrescriptionForm({
  episodeId,
  onCreated,
}: {
  episodeId: string;
  onCreated: () => void;
}) {
  const [form, setForm] = useState<FormState>(EMPTY);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const create = useCreatePrescription();

  function set(field: keyof FormState, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  const canSubmit =
    form.medicationName.trim().length > 0 &&
    form.dosage.trim().length > 0 &&
    form.frequency.trim().length > 0;

  async function handleSubmit() {
    setErrorMsg(null);
    try {
      await create.mutateAsync({
        episodeId,
        medicationName: form.medicationName.trim(),
        medicationCode: form.medicationCode.trim() || undefined,
        dosage: form.dosage.trim(),
        frequency: form.frequency.trim(),
        duration: form.duration.trim() || undefined,
        refills: parseInt(form.refills, 10) || 0,
        instructions: form.instructions.trim() || undefined,
      });
      setForm(EMPTY);
      onCreated();
    } catch (e) {
      setErrorMsg(e instanceof ApiError ? e.message : "Could not create prescription. Try again.");
    }
  }

  return (
    <div className="mt-4 rounded-xl border border-line bg-canvas p-4">
      <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-brand-700">New prescription</p>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <label className={LABEL_CLASS}>Medication name *</label>
          <input
            className={FIELD_CLASS}
            placeholder="e.g. Amoxicillin"
            value={form.medicationName}
            onChange={(e) => set("medicationName", e.target.value)}
            disabled={create.isPending}
          />
        </div>

        <div>
          <label className={LABEL_CLASS}>Medication code (DIN/NPN)</label>
          <input
            className={FIELD_CLASS}
            placeholder="Optional"
            value={form.medicationCode}
            onChange={(e) => set("medicationCode", e.target.value)}
            disabled={create.isPending}
          />
        </div>

        <div>
          <label className={LABEL_CLASS}>Dosage *</label>
          <input
            className={FIELD_CLASS}
            placeholder="e.g. 500 mg"
            value={form.dosage}
            onChange={(e) => set("dosage", e.target.value)}
            disabled={create.isPending}
          />
        </div>

        <div>
          <label className={LABEL_CLASS}>Frequency *</label>
          <input
            className={FIELD_CLASS}
            placeholder="e.g. Three times daily"
            value={form.frequency}
            onChange={(e) => set("frequency", e.target.value)}
            disabled={create.isPending}
          />
        </div>

        <div>
          <label className={LABEL_CLASS}>Duration</label>
          <input
            className={FIELD_CLASS}
            placeholder="e.g. 7 days"
            value={form.duration}
            onChange={(e) => set("duration", e.target.value)}
            disabled={create.isPending}
          />
        </div>

        <div>
          <label className={LABEL_CLASS}>Refills</label>
          <input
            type="number"
            min="0"
            className={FIELD_CLASS}
            value={form.refills}
            onChange={(e) => set("refills", e.target.value)}
            disabled={create.isPending}
          />
        </div>

        <div className="sm:col-span-2">
          <label className={LABEL_CLASS}>Instructions / notes</label>
          <textarea
            rows={2}
            className={FIELD_CLASS}
            placeholder="e.g. Take with food. Do not combine with alcohol."
            value={form.instructions}
            onChange={(e) => set("instructions", e.target.value)}
            disabled={create.isPending}
          />
        </div>
      </div>

      {errorMsg && (
        <p role="alert" className="mt-3 flex items-center gap-1.5 text-sm text-[--color-urgency-emergency]">
          <AlertTriangle className="size-4 shrink-0" /> {errorMsg}
        </p>
      )}

      <Button
        className="mt-4"
        disabled={!canSubmit || create.isPending}
        onClick={handleSubmit}
      >
        {create.isPending ? <Loader2 className="size-4 animate-spin" /> : <Pill className="size-4" />}
        Create prescription
      </Button>

      {create.data && (
        <p className="mt-2 flex items-center gap-1.5 text-sm font-medium text-brand-700">
          <CheckCircle2 className="size-4" /> Prescription created — sign it below to proceed.
        </p>
      )}
    </div>
  );
}

// ── Main card ────────────────────────────────────────────────────────────────

export function PrescriptionCard({ episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const [showForm, setShowForm] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Prescriptions hang off the episode now, so a physician can prescribe without a care plan.
  const { data: prescriptions, isLoading } = useEpisodePrescriptions(episodeId);
  const sign = useSignPrescription();
  const send = useSendPrescription();
  const cancel = useCancelPrescription();

  const anyBusy = sign.isPending || send.isPending || cancel.isPending;

  async function handleSign(id: string) {
    setActionError(null);
    try {
      await sign.mutateAsync(id);
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "Sign failed. Try again.");
    }
  }

  async function handleSend(id: string) {
    setActionError(null);
    try {
      await send.mutateAsync({ id });
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "Send failed. Try again.");
    }
  }

  async function handleCancel(id: string) {
    setActionError(null);
    try {
      await cancel.mutateAsync(id);
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "Cancel failed. Try again.");
    }
  }

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 font-bold text-brand-900">
          <Pill className="size-4 text-brand-600" /> Prescriptions
        </h3>
        {!showForm && (
          <Button size="sm" variant="outline" onClick={() => setShowForm(true)}>
            <Pill className="size-4" /> Add
          </Button>
        )}
      </div>

      {showForm && (
        <NewPrescriptionForm
          episodeId={episodeId}
          onCreated={() => setShowForm(false)}
        />
      )}

      {isLoading && (
        <div className="mt-4 flex items-center gap-2 text-muted">
          <Loader2 className="size-4 animate-spin" /> Loading prescriptions…
        </div>
      )}

      {actionError && (
        <p role="alert" className="mt-3 flex items-center gap-1.5 text-sm text-[--color-urgency-emergency]">
          <AlertTriangle className="size-4 shrink-0" /> {actionError}
        </p>
      )}

      {!isLoading && prescriptions && prescriptions.length === 0 && !showForm && (
        <p className="mt-4 text-sm text-muted">No prescriptions yet for this episode.</p>
      )}

      {!isLoading && prescriptions && prescriptions.length > 0 && (
        <ul className="mt-4 space-y-3">
          {prescriptions.map((rx) => (
            <PrescriptionRow
              key={rx.id}
              rx={rx}
              onSign={handleSign}
              onSend={handleSend}
              onCancel={handleCancel}
              busy={anyBusy}
            />
          ))}
        </ul>
      )}

      {/* user is consumed in the form (disabled states) and future physician-only gates */}
      {user === null && null}
    </section>
  );
}
