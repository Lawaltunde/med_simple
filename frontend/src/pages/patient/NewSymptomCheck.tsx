import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Camera, Mic, MessageSquareText, Loader2, AlertCircle } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useSubmitSymptomCheck } from "@/features/symptom/hooks";
import { ApiError } from "@/lib/api";

const MIN_CHARS = 10;

export function NewSymptomCheck() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const submit = useSubmitSymptomCheck();
  const [description, setDescription] = useState("");
  const [touched, setTouched] = useState(false);

  const tooShort = description.trim().length < MIN_CHARS;

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setTouched(true);
    if (tooShort || !user) return;
    submit.mutate(
      { patientId: user.id, description: description.trim() },
      { onSuccess: (episode) => navigate(`/app/episodes/${episode.id}`) }
    );
  }

  const errorMessage =
    submit.error instanceof ApiError ? submit.error.message : submit.error ? "Something went wrong. Try again." : null;

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">New symptom check</h2>
        <p className="mt-1 text-muted">
          Describe what's wrong in your own words. We'll sort the urgency and a physician reviews every answer.
        </p>
      </header>

      <form onSubmit={onSubmit} className="space-y-5">
        <Textarea
          name="description"
          label="What's going on?"
          placeholder="e.g. I've had a sore throat and fever for two days, and it hurts to swallow."
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={6}
          disabled={submit.isPending}
          error={touched && tooShort ? `Please add a little more detail (at least ${MIN_CHARS} characters).` : undefined}
          hint={!touched || !tooShort ? "The more detail you give, the better the triage." : undefined}
        />

        {errorMessage && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-xl border border-[--color-urgency-emergency]/30 bg-[--color-urgency-emergency]/5 p-3 text-sm text-[--color-urgency-emergency]"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={submit.isPending}>
          {submit.isPending ? (
            <>
              <Loader2 className="size-5 animate-spin" /> Checking your symptoms…
            </>
          ) : (
            <>
              <MessageSquareText className="size-5" /> Submit for triage
            </>
          )}
        </Button>
      </form>

      {/* Voice and photo intake exist in the backend pipeline but need a media-upload
          path that isn't wired to the browser yet. Shown so the option is visible,
          disabled so we don't promise something that doesn't work. */}
      <div className="mt-8">
        <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">Other ways to ask (coming soon)</h3>
        <div className="grid gap-3 sm:grid-cols-2">
          {[
            { icon: Camera, title: "Photograph it", body: "Snap a rash, cut, or swelling." },
            { icon: Mic, title: "Speak it", body: "Record a voice note instead." },
          ].map((c) => (
            <div
              key={c.title}
              aria-disabled
              className="flex cursor-not-allowed items-center gap-3 rounded-2xl border border-dashed border-line bg-white/60 p-4 opacity-70"
            >
              <span className="inline-flex size-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                <c.icon className="size-5" />
              </span>
              <div>
                <h4 className="text-sm font-bold text-brand-900">{c.title}</h4>
                <p className="text-xs text-muted">{c.body}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
