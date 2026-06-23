import { Loader2, CalendarDays } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { usePatientAppointments } from "@/features/appointment/hooks";
import { AppointmentList } from "@/features/appointment/display";

export function AppointmentsPage() {
  const { user } = useAuth();
  const { data: appointments, isLoading } = usePatientAppointments(user?.id);

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Appointments</h2>
        <p className="mt-1 text-muted">Visits and follow-ups your physician has booked.</p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !appointments || appointments.length === 0 ? (
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
        <AppointmentList appointments={appointments} audience="patient" />
      )}
    </div>
  );
}
