import { useState } from "react";
import { Send, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatDateTime } from "@/features/symptom/display";
import { useAddFollowUp, useRespondToFollowUp } from "./hooks";
import type { CarePlan, FollowUp } from "./types";

/**
 * Bidirectional follow-up thread shared by the patient and clinician views. Each item is
 * a message and an optional reply; whoever didn't write the message can answer it. A
 * compose box at the bottom starts a new message.
 */
export function FollowUpThread({ plan, currentUserId }: { plan: CarePlan; currentUserId: string }) {
  const add = useAddFollowUp(plan.id, plan.patientId);
  const [message, setMessage] = useState("");
  const viewerIsPhysician = currentUserId === plan.physicianId;

  function authorLabel(userId: string | null, system: boolean): string {
    if (system) return "MedSimple";
    if (userId === plan.physicianId) return "Physician";
    if (userId === plan.patientId) return "Patient";
    return "Unknown";
  }

  function send() {
    if (message.trim().length === 0) return;
    add.mutate(
      { initiatedBy: currentUserId, message: message.trim() },
      { onSuccess: () => setMessage("") }
    );
  }

  return (
    <div className="space-y-4">
      <h3 className="font-bold text-brand-900">Messages</h3>

      {plan.followUps.length === 0 ? (
        <p className="text-sm text-muted">No messages yet.</p>
      ) : (
        <ul className="space-y-4">
          {plan.followUps.map((f) => (
            <li key={f.id} className="space-y-2">
              <Bubble side={f.initiatedBy === currentUserId ? "right" : "left"}>
                <p className="text-xs font-semibold opacity-70">{authorLabel(f.initiatedBy, f.system)}</p>
                <p>{f.message}</p>
                <p className="mt-1 text-[0.65rem] opacity-60">{formatDateTime(f.createdAt)}</p>
              </Bubble>

              {f.response ? (
                <Bubble side={f.respondedBy === currentUserId ? "right" : "left"}>
                  <p className="text-xs font-semibold opacity-70">{authorLabel(f.respondedBy, false)}</p>
                  <p>{f.response}</p>
                  {f.respondedAt && <p className="mt-1 text-[0.65rem] opacity-60">{formatDateTime(f.respondedAt)}</p>}
                </Bubble>
              ) : (
                // Only the person who did NOT write the message can reply to it.
                f.initiatedBy !== currentUserId && (
                  <ReplyBox plan={plan} followUp={f} currentUserId={currentUserId} />
                )
              )}
            </li>
          ))}
        </ul>
      )}

      {/* Compose a new message */}
      <div className="flex items-end gap-2 border-t border-line pt-4">
        <textarea
          aria-label="New message"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={2}
          placeholder={viewerIsPhysician ? "Message your patient…" : "Ask your care team a question…"}
          className="min-h-11 flex-1 resize-y rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25"
        />
        <button
          onClick={send}
          disabled={add.isPending || message.trim().length === 0}
          aria-label="Send message"
          className="grid size-11 shrink-0 place-content-center rounded-xl bg-brand-600 text-white transition-colors hover:bg-brand-700 disabled:opacity-50"
        >
          {add.isPending ? <Loader2 className="size-5 animate-spin" /> : <Send className="size-5" />}
        </button>
      </div>
    </div>
  );
}

function ReplyBox({
  plan,
  followUp,
  currentUserId,
}: {
  plan: CarePlan;
  followUp: FollowUp;
  currentUserId: string;
}) {
  const respond = useRespondToFollowUp(plan.id, plan.patientId);
  const [text, setText] = useState("");
  return (
    <div className="ml-6 flex items-end gap-2">
      <input
        aria-label="Reply"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Reply…"
        className="h-10 flex-1 rounded-xl border border-line bg-white px-3 text-sm text-ink focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25"
      />
      <button
        onClick={() =>
          text.trim() &&
          respond.mutate(
            { followUpId: followUp.id, respondedBy: currentUserId, response: text.trim() },
            { onSuccess: () => setText("") }
          )
        }
        disabled={respond.isPending || text.trim().length === 0}
        className="grid size-10 shrink-0 place-content-center rounded-xl bg-brand-600 text-white hover:bg-brand-700 disabled:opacity-50"
        aria-label="Send reply"
      >
        {respond.isPending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
      </button>
    </div>
  );
}

function Bubble({ side, children }: { side: "left" | "right"; children: React.ReactNode }) {
  return (
    <div className={cn("flex", side === "right" ? "justify-end" : "justify-start")}>
      <div
        className={cn(
          "max-w-[80%] rounded-2xl px-4 py-2 text-sm",
          side === "right" ? "bg-brand-600 text-white" : "border border-line bg-white text-ink"
        )}
      >
        {children}
      </div>
    </div>
  );
}
