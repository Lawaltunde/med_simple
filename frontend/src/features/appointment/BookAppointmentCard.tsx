import { useState } from "react";
import { CalendarPlus, Loader2, CheckCircle2 } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import type { Episode } from "@/features/symptom/types";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useCreateAppointment } from "./hooks";

// Physician books a follow-up for the episode. Patient + clinic are derived server-side.
export function BookAppointmentCard({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const create = useCreateAppointment(episode.patientId);
  const [when, setWhen] = useState("");
  const [duration, setDuration] = useState("15");
  const [reason, setReason] = useState("");

  if (user?.role !== "PHYSICIAN") return null;

  const valid = when.length > 0;

  function book() {
    if (!user || !valid) return;
    create.mutate(
      {
        episodeId,
        physicianId: user.id,
        scheduledAt: new Date(when).toISOString(),
        durationMinutes: Number(duration) || 15,
        reason: reason.trim() || undefined,
      },
      { onSuccess: () => { setWhen(""); setReason(""); } }
    );
  }

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <h3 className="flex items-center gap-2 font-bold text-brand-900">
        <CalendarPlus className="size-4 text-brand-600" /> Book an appointment
      </h3>
      <p className="mt-1 text-sm text-muted">Schedule a visit or follow-up. The patient is notified and can add it to their calendar.</p>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-ink">Date & time</span>
          <input
            type="datetime-local"
            value={when}
            onChange={(e) => setWhen(e.target.value)}
            disabled={create.isPending}
            className="h-11 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink focus:border-brand-400 focus:outline-none"
          />
        </label>
        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-ink">Duration (minutes)</span>
          <input
            type="number"
            min={5}
            step={5}
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
            disabled={create.isPending}
            className="h-11 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink focus:border-brand-400 focus:outline-none"
          />
        </label>
      </div>

      <Textarea
        name="reason"
        className="mt-3"
        rows={2}
        placeholder="Reason (e.g. Review throat swab results)"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        disabled={create.isPending}
      />

      {create.isSuccess && (
        <p className="mt-3 flex items-center gap-2 text-sm font-medium text-brand-700">
          <CheckCircle2 className="size-4" /> Appointment booked and the patient was notified.
        </p>
      )}

      <Button className="mt-4" disabled={!valid || create.isPending || !user} onClick={book}>
        {create.isPending ? <Loader2 className="size-4 animate-spin" /> : <CalendarPlus className="size-4" />}
        Book appointment
      </Button>
    </section>
  );
}
