import { useState } from "react";
import { Loader2, CalendarDays } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { usePatientAppointments } from "@/features/appointment/hooks";
import { AppointmentList } from "@/features/appointment/display";
import { MonthCalendar } from "@/features/appointment/MonthCalendar";

function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

export function AppointmentsPage() {
  const { user } = useAuth();
  const { data: appointments, isLoading } = usePatientAppointments(user?.id);
  const [day, setDay] = useState<Date | null>(null);

  const hasAny = !!appointments && appointments.length > 0;
  const marked = (appointments ?? []).map((a) => new Date(a.scheduledAt));
  const shown = day
    ? (appointments ?? []).filter((a) => sameDay(new Date(a.scheduledAt), day))
    : (appointments ?? []);

  return (
    <div className="mx-auto max-w-3xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Appointments</h2>
        <p className="mt-1 text-muted">Visits and follow-ups your physician has booked.</p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !hasAny ? (
        <div className="flex flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <CalendarDays className="size-6" />
          </span>
          <p className="mt-5 font-medium text-ink">No appointments yet</p>
          <p className="mt-1 max-w-xs text-sm text-muted">
            When your physician books a visit or follow-up, it'll appear here with an option to add it
            to your calendar.
          </p>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-[minmax(0,18rem)_minmax(0,1fr)]">
          <div>
            <MonthCalendar selected={day} onSelect={setDay} marked={marked} />
            {day && (
              <button
                type="button"
                onClick={() => setDay(null)}
                className="mt-2 text-sm font-medium text-brand-700 hover:text-brand-900"
              >
                Show all appointments
              </button>
            )}
          </div>
          <div>
            {shown.length === 0 ? (
              <p className="rounded-2xl border border-line bg-white px-5 py-8 text-center text-sm text-muted">
                No appointments on this day.
              </p>
            ) : (
              <AppointmentList appointments={shown} audience="patient" />
            )}
          </div>
        </div>
      )}
    </div>
  );
}
