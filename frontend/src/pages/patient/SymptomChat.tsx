import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Bot, Send, Loader2, AlertTriangle, ShieldCheck, Stethoscope } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { Button } from "@/components/ui/button";
import { sendSymptomChat } from "@/features/chat/api";
import type { ChatTurn, SymptomChatResponse } from "@/features/chat/types";
import { submitTextSymptomCheck } from "@/features/symptom/api";

const GREETING: ChatTurn = {
  role: "assistant",
  content:
    "Hi, I'm the MedSimple Assistant. Tell me what you're feeling and I'll help you understand " +
    "it and what to do next. I can't diagnose, and a clinician always reviews your case.",
};

export function SymptomChat() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [turns, setTurns] = useState<ChatTurn[]>([GREETING]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [last, setLast] = useState<SymptomChatResponse | null>(null);
  const [handingOff, setHandingOff] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [turns, sending]);

  async function send() {
    const text = input.trim();
    if (!text || sending) return;
    const next = [...turns, { role: "user" as const, content: text }];
    setTurns(next);
    setInput("");
    setSending(true);
    try {
      // Send only the real conversation (drop the canned greeting).
      const res = await sendSymptomChat(next.filter((t) => t !== GREETING));
      setTurns((t) => [...t, { role: "assistant", content: res.reply }]);
      setLast(res);
    } catch {
      setTurns((t) => [
        ...t,
        { role: "assistant", content: "Sorry, I couldn't respond. If this is urgent, call 911." },
      ]);
    } finally {
      setSending(false);
    }
  }

  async function sendToDoctor() {
    if (!user || !last) return;
    setHandingOff(true);
    try {
      const episode = await submitTextSymptomCheck({
        patientId: user.id,
        description: last.symptomSummary,
      });
      navigate(`/app/episodes/${episode.id}`);
    } finally {
      setHandingOff(false);
    }
  }

  return (
    <div className="mx-auto flex h-[calc(100dvh-7rem)] max-w-2xl flex-col">
      <header className="mb-3">
        <h1 className="flex items-center gap-2 text-xl font-bold text-brand-900">
          <Bot className="size-5 text-brand-600" /> AI Symptom Assistant
        </h1>
        <p className="mt-1 text-sm text-muted">
          Describe how you feel. The assistant gives general guidance, never a diagnosis.
        </p>
      </header>

      {last?.emergency && (
        <div
          role="alert"
          className="mb-3 flex items-start gap-2 rounded-xl border border-[--color-urgency-emergency] bg-red-50 p-3 text-sm font-medium text-[--color-urgency-emergency]"
        >
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          This may be an emergency. Call 911 or go to the nearest emergency department now.
        </div>
      )}

      <div className="flex-1 space-y-3 overflow-y-auto rounded-2xl border border-line bg-canvas p-4">
        {turns.map((t, i) => (
          <div key={i} className={t.role === "user" ? "flex justify-end" : "flex justify-start"}>
            <div
              className={
                t.role === "user"
                  ? "max-w-[80%] rounded-2xl rounded-br-sm bg-brand-600 px-4 py-2 text-sm text-white"
                  : "max-w-[80%] rounded-2xl rounded-bl-sm border border-line bg-white px-4 py-2 text-sm text-ink"
              }
            >
              {t.content}
            </div>
          </div>
        ))}
        {sending && (
          <div className="flex justify-start">
            <div className="rounded-2xl rounded-bl-sm border border-line bg-white px-4 py-2 text-sm text-muted">
              <Loader2 className="inline size-4 animate-spin" /> thinking…
            </div>
          </div>
        )}
        <div ref={endRef} />
      </div>

      {last && !last.emergency && (
        <div className="mt-3">
          <Button onClick={sendToDoctor} disabled={handingOff} className="w-full" variant="secondary">
            {handingOff ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <Stethoscope className="size-4" />
            )}
            Send these symptoms to a doctor for review
          </Button>
        </div>
      )}

      <div className="mt-3 flex items-end gap-2">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              send();
            }
          }}
          rows={1}
          placeholder="Describe your symptoms…"
          aria-label="Describe your symptoms"
          className="max-h-32 flex-1 resize-none rounded-xl border border-line bg-white px-4 py-3 text-sm outline-none focus:border-brand-400"
        />
        <Button onClick={send} disabled={sending || !input.trim()} aria-label="Send">
          <Send className="size-4" />
        </Button>
      </div>

      <p className="mt-2 flex items-center gap-1.5 text-xs text-muted">
        <ShieldCheck className="size-3.5 text-brand-500" />
        General health information, not a diagnosis. Personal details are removed before AI sees
        your message. In an emergency, call 911.
      </p>
    </div>
  );
}
