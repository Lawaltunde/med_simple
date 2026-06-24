import { useEffect, useRef, useState } from "react";
import { Mic, Square, Trash2 } from "lucide-react";

type RecorderState = "idle" | "recording" | "recorded";

interface VoiceRecorderProps {
  // Called with the recorded audio blob, or null when cleared.
  onChange: (blob: Blob | null) => void;
  disabled?: boolean;
}

// Pick a mime type the browser can record AND the backend accepts (webm/mp4/wav).
function pickMimeType(): string {
  const candidates = ["audio/webm", "audio/mp4", "audio/wav"];
  if (typeof MediaRecorder === "undefined" || !MediaRecorder.isTypeSupported) return "audio/webm";
  return candidates.find((t) => MediaRecorder.isTypeSupported(t)) ?? "audio/webm";
}

function fmt(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

/**
 * Voice intake: record via the MediaRecorder API, stop, play back, and re-record. Produces a
 * Blob the caller uploads. Releases the microphone stream as soon as recording stops, and on
 * unmount, so the device's mic indicator never lingers.
 */
export function VoiceRecorder({ onChange, disabled }: VoiceRecorderProps) {
  const [state, setState] = useState<RecorderState>("idle");
  const [elapsed, setElapsed] = useState(0);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function stopStream() {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  }

  function stopTimer() {
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = null;
  }

  // Clean up everything on unmount: timer, mic stream, and the playback object URL.
  useEffect(() => {
    return () => {
      stopTimer();
      stopStream();
      if (audioUrl) URL.revokeObjectURL(audioUrl);
    };
  }, [audioUrl]);

  async function start() {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      const mimeType = pickMimeType();
      const recorder = new MediaRecorder(stream, { mimeType });
      chunksRef.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
      };
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: mimeType });
        if (audioUrl) URL.revokeObjectURL(audioUrl);
        setAudioUrl(URL.createObjectURL(blob));
        setState("recorded");
        onChange(blob);
        stopStream();
      };

      recorder.start();
      recorderRef.current = recorder;
      setElapsed(0);
      setState("recording");
      timerRef.current = setInterval(() => setElapsed((e) => e + 1), 1000);
    } catch {
      setError("Microphone access was blocked. Allow it in your browser to record a voice note.");
    }
  }

  function stop() {
    stopTimer();
    recorderRef.current?.stop();
  }

  function reset() {
    stopTimer();
    stopStream();
    if (audioUrl) URL.revokeObjectURL(audioUrl);
    setAudioUrl(null);
    setElapsed(0);
    setState("idle");
    onChange(null);
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-col items-center gap-3 rounded-2xl border border-line bg-white/60 px-6 py-8">
        {state === "idle" && (
          <button
            type="button"
            onClick={start}
            disabled={disabled}
            className="flex flex-col items-center gap-2 text-muted transition-colors hover:text-brand-700 disabled:opacity-50"
          >
            <span className="inline-flex size-14 items-center justify-center rounded-full bg-brand-600 text-white">
              <Mic className="size-6" />
            </span>
            <span className="text-sm font-medium text-brand-900">Tap to record</span>
            <span className="text-xs">Describe how you feel in your own voice.</span>
          </button>
        )}

        {state === "recording" && (
          <button
            type="button"
            onClick={stop}
            disabled={disabled}
            className="flex flex-col items-center gap-2"
          >
            <span className="relative inline-flex size-14 items-center justify-center rounded-full bg-[--color-urgency-emergency] text-white">
              <span className="absolute inset-0 animate-ping rounded-full bg-[--color-urgency-emergency]/40" />
              <Square className="size-5" />
            </span>
            <span className="text-sm font-semibold text-[--color-urgency-emergency]">Recording {fmt(elapsed)}</span>
            <span className="text-xs text-muted">Tap to stop</span>
          </button>
        )}

        {state === "recorded" && audioUrl && (
          <div className="w-full space-y-3">
            <audio src={audioUrl} controls className="w-full" data-testid="voice-playback" />
            <button
              type="button"
              onClick={reset}
              disabled={disabled}
              className="mx-auto flex items-center gap-1.5 text-sm text-muted hover:text-ink"
            >
              <Trash2 className="size-4" /> Record again
            </button>
          </div>
        )}
      </div>

      {error && (
        <p role="alert" className="text-sm text-[--color-urgency-emergency]">
          {error}
        </p>
      )}
    </div>
  );
}
