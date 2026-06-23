import { Loader2, ShieldCheck, Sparkles } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { useConsents, useSetConsent, isConsentActive } from "@/features/consent/hooks";
import { cn } from "@/lib/utils";

export function PrivacyPage() {
  const { user } = useAuth();
  const { data: consents, isLoading } = useConsents(user?.id);
  const setConsent = useSetConsent(user?.id ?? "");
  const aiTraining = isConsentActive(consents, "AI_TRAINING");

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Privacy & AI</h2>
        <p className="mt-1 text-muted">Control how your data is used. You can change this anytime.</p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : (
        <section className="rounded-2xl border border-line bg-white p-6">
          <div className="flex items-start gap-4">
            <span className="inline-flex size-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
              <Sparkles className="size-5" />
            </span>
            <div className="min-w-0 flex-1">
              <h3 className="font-bold text-brand-900">Help improve care suggestions</h3>
              <p className="mt-1 text-sm text-muted">
                Allow your de-identified, physician-approved care plans to help the AI suggest better
                plans for patients with similar symptoms. Your name and identifiers are never used,
                and this never affects your own care. Off by default.
              </p>

              <button
                role="switch"
                aria-checked={aiTraining}
                aria-label="Allow my data to improve care suggestions"
                disabled={setConsent.isPending || !user}
                onClick={() => setConsent.mutate({ consentType: "AI_TRAINING", grant: !aiTraining })}
                className={cn(
                  "mt-4 inline-flex h-7 w-12 items-center rounded-full p-0.5 transition-colors",
                  aiTraining ? "bg-brand-600" : "bg-line"
                )}
              >
                <span
                  className={cn(
                    "grid size-6 place-content-center rounded-full bg-white shadow transition-transform",
                    aiTraining && "translate-x-5"
                  )}
                >
                  {setConsent.isPending && <Loader2 className="size-3.5 animate-spin text-muted" />}
                </span>
              </button>
              <p className="mt-2 text-xs font-medium text-muted">
                {aiTraining ? "On — thank you for helping improve care." : "Off"}
              </p>
            </div>
          </div>

          <div className="mt-5 flex items-start gap-2 border-t border-line pt-4 text-xs text-muted">
            <ShieldCheck className="mt-0.5 size-4 shrink-0 text-brand-600" />
            <p>
              Turning this off stops any future use of your data. Consent is recorded as an
              append-only log for your records.
            </p>
          </div>
        </section>
      )}
    </div>
  );
}
