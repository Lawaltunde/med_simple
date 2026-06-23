import { Loader2, FlaskConical, Sparkles, UserCheck } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { usePatientResults } from "@/features/diagnostic/hooks";
import { formatDateTime } from "@/features/symptom/display";
import type { PatientResult } from "@/features/diagnostic/types";

function ResultCard({ result }: { result: PatientResult }) {
  return (
    <li className="rounded-2xl border border-line bg-white p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="font-semibold text-ink">{result.testType}</p>
          <p className="mt-0.5 text-xs text-muted">
            Released {formatDateTime(result.releasedToPatientAt)}
          </p>
        </div>
        <span className="inline-flex items-center rounded-full bg-green-50 px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide text-green-700">
          Released
        </span>
      </div>

      {result.aiSummary && (
        <div className="mt-4 rounded-xl bg-brand-50 p-4">
          <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-brand-700">
            <Sparkles className="size-3.5" /> Summary
          </p>
          <p className="mt-1.5 text-[0.9rem] leading-relaxed text-ink">{result.aiSummary}</p>
        </div>
      )}

      {result.physicianInterpretation && (
        <div className="mt-3 rounded-xl bg-canvas p-4">
          <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-muted">
            <UserCheck className="size-3.5" /> Physician's note
          </p>
          <p className="mt-1.5 text-[0.9rem] leading-relaxed text-ink">
            {result.physicianInterpretation}
          </p>
        </div>
      )}

      {!result.aiSummary && !result.physicianInterpretation && (
        <p className="mt-3 text-sm text-muted">No additional notes from your care team yet.</p>
      )}
    </li>
  );
}

export function ResultsPage() {
  const { user } = useAuth();
  // Patient endpoint: /api/v1/patients/{patientId}/results
  // Returns only released results (released_to_patient_at non-NULL).
  // Gate is enforced server-side in DiagnosticService — we consume the released endpoint only.
  const { data: results, isLoading } = usePatientResults(user?.id);

  // Sort newest first (releasedToPatientAt descending).
  const sorted = results
    ? [...results].sort(
        (a, b) =>
          new Date(b.releasedToPatientAt).getTime() -
          new Date(a.releasedToPatientAt).getTime()
      )
    : [];

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Test results</h2>
        <p className="mt-1 text-muted">
          Lab and imaging results released to you by your care team. Updated automatically.
        </p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : sorted.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <FlaskConical className="size-6" />
          </span>
          <p className="mt-5 font-medium text-ink">No results yet</p>
          <p className="mt-1 max-w-xs text-sm text-muted">
            Released lab and imaging results will appear here once your physician shares them.
          </p>
        </div>
      ) : (
        <ul className="space-y-4">
          {sorted.map((r) => (
            <ResultCard key={r.id} result={r} />
          ))}
        </ul>
      )}
    </div>
  );
}
