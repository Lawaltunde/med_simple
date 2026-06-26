import { useState } from "react";
import { CalendarPlus, Loader2, CheckCircle2, Bot } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import type { Episode } from "@/features/symptom/types";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useEpisodeSuggestion } from "@/features/careplan/hooks";
import { useCreateAppointment, usePatientAppointments } from "./hooks";
import { MonthCalendar } from "./MonthCalendar";

function addDays(base: Date, days: number): Date {
  const d = new Date(base);
  d.setDate(d.getDate() + days);
  return d;
}

function formatDay(d: Date): string {
  return d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}

/**
 * Physician books a follow-up for the episode. The AI's suggested interval shades a *recommended
 * window* on the calendar; the physician clicks the actual day. The AI never commits a date.
 */
export function BookAppointmentCard({ episode, episodeId }: { episode: Episode; episodeId: string }) {
  const { user } = useAuth();
  const { data: suggestion } = useEpisodeSuggestion(episodeId);
  const { data: existing } = usePatientAppointments(episode.patientId);
  const create = useCreateAppointment(episode.patientId);

  const [day, setDay] = useState<Date | null>(null);
  const [time, setTime] = useState("09:00");
  const [duration, setDuration] = useState("15");
  const [reason, setReason] = useState("");

  if (user?.role !== "PHYSICIAN") return null;

  const today = new Date();
  const aiFollowUpDays = suggestion?.suggestedFollowUpDays ?? null;

  // Clinical timing intent -> a shaded window, not a committed date. ~a week around the suggestion.
  const recommended = aiFollowUpDays
    ? { from: addDays(today, Math.max(0, aiFollowUpDays - 3)), to: addDays(today, aiFollowUpDays + 3) }
    : null;
  const recommendedCentre = aiFollowUpDays ? addDays(today, aiFollowUpDays) : null;

  // Existing appointments for this patient, dotted on the calendar so the physician avoids clashes.
  const marked = (existing ?? []).map((a) => new Date(a.scheduledAt));

  const valid = day !== null && time.length > 0;

  function book() {
    if (!user || !day || !time) return;
    const [hh, mm] = time.split(":").map(Number);
    const scheduled = new Date(day.getFullYear(), day.getMonth(), day.getDate(), hh, mm, 0, 0);
    create.mutate(
      {
        episodeId,
        physicianId: user.id,
        scheduledAt: scheduled.toISOString(),
        durationMinutes: Number(duration) || 15,
        reason: reason.trim() || undefined,
      },
      { onSuccess: () => { setDay(null); setReason(""); } },
    );
  }

  return (
    <section className="rounded-2xl border border-line bg-white p-6">
      <h3 className="flex items-center gap-2 font-bold text-brand-900">
        <CalendarPlus className="size-4 text-brand-600" /> Book an appointment
      </h3>
      <p className="mt-1 text-sm text-muted">
        Schedule a visit or follow-up. The patient is notified and can add it to their calendar.
      </p>

      {recommendedCentre && (
        <p className="mt-3 flex items-center gap-1.5 rounded-xl border border-brand-200 bg-brand-50/60 p-3 text-sm text-brand-800">
          <Bot className="size-4 shrink-0 text-brand-600" />
          AI recommends a follow-up around {formatDay(recommendedCentre)} (~{aiFollowUpDays} day
          {aiFollowUpDays === 1 ? "" : "s"}). Pick the day that fits.
        </p>
      )}

      <div className="mt-4 grid gap-4 sm:grid-cols-[minmax(0,1fr)_minmax(0,14rem)]">
        <MonthCalendar
          selected={day}
          onSelect={setDay}
          minDate={today}
          recommended={recommended}
          marked={marked}
        />

        <div className="space-y-3">
          <div>
            <span className="mb-1.5 block text-sm font-medium text-ink">Selected day</span>
            <p className="rounded-xl border border-line bg-brand-50/40 px-3 py-2 text-sm text-ink">
              {day ? formatDay(day) : <span className="text-muted">Pick a day on the calendar</span>}
            </p>
          </div>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-ink">Time</span>
            <input
              type="time"
              value={time}
              onChange={(e) => setTime(e.target.value)}
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
