import { useState } from "react";
import { CalendarPlus, Loader2, Clock } from "lucide-react";
import type { Appointment, AppointmentStatus } from "./types";
import { downloadIcs } from "./api";
import { cn } from "@/lib/utils";

const STATUS_META: Record<AppointmentStatus, { label: string; className: string }> = {
  SCHEDULED: { label: "Scheduled", className: "bg-brand-100 text-brand-700" },
  COMPLETED: { label: "Completed", className: "bg-green-100 text-green-800" },
  CANCELLED: { label: "Cancelled", className: "bg-canvas text-muted line-through" },
  NO_SHOW: { label: "No show", className: "bg-canvas text-muted" },
};

function AddToCalendarButton({ id }: { id: string }) {
  const [busy, setBusy] = useState(false);
  async function add() {
    setBusy(true);
    try {
      const blob = await downloadIcs(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "medsimple-appointment.ics";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } finally {
      setBusy(false);
    }
  }
  return (
    <button
      onClick={add}
      disabled={busy}
      className="inline-flex items-center gap-1.5 rounded-full border border-line px-3 py-1.5 text-xs font-semibold text-brand-700 hover:bg-brand-50"
    >
      {busy ? <Loader2 className="size-3.5 animate-spin" /> : <CalendarPlus className="size-3.5" />}
      Add to calendar
    </button>
  );
}

// Shared calendar-style list used by both the patient app and the console. `audience`
// decides whether each row leads with the physician (patient view) or the patient (console).
export function AppointmentList({
  appointments,
  audience,
}: {
  appointments: Appointment[];
  audience: "patient" | "console";
}) {
  return (
    <ul className="space-y-3">
      {appointments.map((a) => {
        const d = new Date(a.scheduledAt);
        const meta = STATUS_META[a.status];
        const who = audience === "patient" ? `Dr. ${a.physicianName}` : a.patientName;
        return (
          <li
            key={a.id}
            className="flex items-stretch gap-4 rounded-2xl border border-line bg-white p-4"
          >
            {/* Calendar-style date block */}
            <div className="grid w-16 shrink-0 place-content-center rounded-xl bg-brand-50 text-center text-brand-700">
              <span className="text-[0.65rem] font-bold uppercase tracking-wide">
                {d.toLocaleDateString(undefined, { month: "short" })}
              </span>
              <span className="text-2xl font-extrabold leading-none">{d.getDate()}</span>
            </div>

            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-ink">{who}</p>
                <span className={cn("rounded-full px-2 py-0.5 text-xs font-bold", meta.className)}>
                  {meta.label}
                </span>
              </div>
              <p className="mt-0.5 flex items-center gap-1.5 text-sm text-muted">
                <Clock className="size-3.5" />
                {d.toLocaleString(undefined, { weekday: "short", hour: "numeric", minute: "2-digit" })}
                {" · "}
                {a.durationMinutes} min · {a.clinicName}
              </p>
              {a.reason && <p className="mt-1 text-sm text-ink">{a.reason}</p>}
              {a.status === "SCHEDULED" && (
                <div className="mt-2">
                  <AddToCalendarButton id={a.id} />
                </div>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
