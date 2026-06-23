import { Loader2, CalendarDays, Building2 } from "lucide-react";
import { useMyClinics } from "@/features/clinician/hooks";
import { useClinicAppointments } from "@/features/appointment/hooks";
import { AppointmentList } from "@/features/appointment/display";

export function ConsoleAppointmentsPage() {
  const { data: clinics, isLoading: clinicsLoading } = useMyClinics();
  const clinic = clinics?.[0];
  const { data: appointments, isLoading } = useClinicAppointments(clinic?.id);

  if (clinicsLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-muted">
        <Loader2 className="size-5 animate-spin" /> Loading…
      </div>
    );
  }

  if (!clinic) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
        <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
          <Building2 className="size-6" />
        </span>
        <h2 className="mt-5 text-lg font-bold text-brand-900">You're not attached to a clinic yet</h2>
        <p className="mt-2 text-sm text-muted">Ask a clinic admin to add you as staff.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <header className="mb-6">
        <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Appointments</h2>
        <p className="mt-1 text-muted">{clinic.name} · upcoming visits and follow-ups.</p>
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !appointments || appointments.length === 0 ? (
        <div className="mx-auto flex max-w-md flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <CalendarDays className="size-6" />
          </span>
          <h2 className="mt-5 text-lg font-bold text-brand-900">No appointments booked</h2>
          <p className="mt-2 text-sm text-muted">Book a follow-up from a patient's episode and it'll show here.</p>
        </div>
      ) : (
        <AppointmentList appointments={appointments} audience="console" />
      )}
    </div>
  );
}
