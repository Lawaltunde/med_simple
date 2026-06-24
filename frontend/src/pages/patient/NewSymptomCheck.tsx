import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Camera, Mic, MessageSquareText, Loader2, AlertCircle } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useSubmitSymptomCheck, useSubmitMediaSymptomCheck } from "@/features/symptom/hooks";
import { PhotoCapture } from "@/features/media/PhotoCapture";
import { VoiceRecorder } from "@/features/media/VoiceRecorder";
import { ApiError } from "@/lib/api";

const MIN_CHARS = 10;

type Mode = "TEXT" | "PHOTO" | "VOICE";

const MODES: { id: Mode; label: string; icon: typeof Camera }[] = [
  { id: "TEXT", label: "Text", icon: MessageSquareText },
  { id: "PHOTO", label: "Photo", icon: Camera },
  { id: "VOICE", label: "Voice", icon: Mic },
];

function isMode(v: string | null): v is Mode {
  return v === "TEXT" || v === "PHOTO" || v === "VOICE";
}

export function NewSymptomCheck() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const initialMode = isMode(params.get("mode")) ? (params.get("mode") as Mode) : "TEXT";
  const [mode, setMode] = useState<Mode>(initialMode);

  const textSubmit = useSubmitSymptomCheck();
  const mediaSubmit = useSubmitMediaSymptomCheck();

  const [description, setDescription] = useState("");
  const [touched, setTouched] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [voice, setVoice] = useState<Blob | null>(null);

  const tooShort = description.trim().length < MIN_CHARS;
  const pending = textSubmit.isPending || mediaSubmit.isPending;

  function switchMode(next: Mode) {
    setMode(next);
    setParams(next === "TEXT" ? {} : { mode: next }, { replace: true });
  }

  function onSuccess(episode: { id: string }) {
    navigate(`/app/episodes/${episode.id}`);
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user) return;

    if (mode === "TEXT") {
      setTouched(true);
      if (tooShort) return;
      textSubmit.mutate({ patientId: user.id, description: description.trim() }, { onSuccess });
      return;
    }
    if (mode === "PHOTO" && photo) {
      mediaSubmit.mutate({ patientId: user.id, inputType: "PHOTO", blob: photo }, { onSuccess });
      return;
    }
    if (mode === "VOICE" && voice) {
      mediaSubmit.mutate({ patientId: user.id, inputType: "VOICE", blob: voice }, { onSuccess });
    }
  }

  const submitDisabled =
    pending || (mode === "PHOTO" && !photo) || (mode === "VOICE" && !voice);

  const err = textSubmit.error ?? mediaSubmit.error;
  const errorMessage = err instanceof ApiError ? err.message : err ? "Something went wrong. Try again." : null;

  const pendingLabel =
    mode === "PHOTO"
      ? "Analyzing your photo…"
      : mode === "VOICE"
        ? "Transcribing your voice note…"
        : "Checking your symptoms…";

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">New symptom check</h2>
        <p className="mt-1 text-muted">
          Tell us what's wrong however suits you. We'll sort the urgency and a physician reviews every answer.
        </p>
      </header>

      {/* Mode switch — text, photo, or voice */}
      <div role="tablist" aria-label="How to describe your symptoms" className="mb-6 grid grid-cols-3 gap-2">
        {MODES.map((m) => {
          const active = mode === m.id;
          return (
            <button
              key={m.id}
              role="tab"
              type="button"
              aria-selected={active}
              onClick={() => switchMode(m.id)}
              disabled={pending}
              className={[
                "flex items-center justify-center gap-2 rounded-xl border px-3 py-2.5 text-sm font-semibold transition-colors disabled:opacity-50",
                active
                  ? "border-brand-300 bg-brand-50 text-brand-700"
                  : "border-line bg-white text-muted hover:border-brand-200 hover:text-brand-700",
              ].join(" ")}
            >
              <m.icon className="size-4" /> {m.label}
            </button>
          );
        })}
      </div>

      <form onSubmit={onSubmit} className="space-y-5">
        {mode === "TEXT" && (
          <Textarea
            name="description"
            label="What's going on?"
            placeholder="e.g. I've had a sore throat and fever for two days, and it hurts to swallow."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={6}
            disabled={pending}
            error={touched && tooShort ? `Please add a little more detail (at least ${MIN_CHARS} characters).` : undefined}
            hint={!touched || !tooShort ? "The more detail you give, the better the triage." : undefined}
          />
        )}

        {mode === "PHOTO" && <PhotoCapture onChange={setPhoto} disabled={pending} />}

        {mode === "VOICE" && <VoiceRecorder onChange={setVoice} disabled={pending} />}

        {errorMessage && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-xl border border-[--color-urgency-emergency]/30 bg-[--color-urgency-emergency]/5 p-3 text-sm text-[--color-urgency-emergency]"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={submitDisabled}>
          {pending ? (
            <>
              <Loader2 className="size-5 animate-spin" /> {pendingLabel}
            </>
          ) : (
            <>
              {mode === "PHOTO" ? (
                <Camera className="size-5" />
              ) : mode === "VOICE" ? (
                <Mic className="size-5" />
              ) : (
                <MessageSquareText className="size-5" />
              )}{" "}
              Submit for triage
            </>
          )}
        </Button>
      </form>
    </div>
  );
}
