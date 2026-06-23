import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AppointmentsPage } from "./AppointmentsPage";
import type { Appointment } from "@/features/appointment/types";

vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

const listMock = vi.fn();
vi.mock("@/features/appointment/api", () => ({
  listPatientAppointments: (...a: unknown[]) => listMock(...a),
  listClinicAppointments: vi.fn(),
  createAppointment: vi.fn(),
  downloadIcs: vi.fn(),
}));

function appt(over: Partial<Appointment>): Appointment {
  return {
    id: "a-1", patientId: "patient-1", physicianId: "doc-1", clinicId: "clinic-1",
    episodeId: "ep-1", carePlanId: null, scheduledAt: "2026-07-01T14:30:00.000Z",
    durationMinutes: 20, reason: "Review throat swab", status: "SCHEDULED",
    clinicName: "Demo Clinic", physicianName: "Dana Okafor", patientName: "Jane Doe",
    createdAt: "2026-06-23T10:00:00Z", ...over,
  };
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <AppointmentsPage />
    </QueryClientProvider>
  );
}

describe("AppointmentsPage", () => {
  beforeEach(() => listMock.mockReset());

  it("shows the empty state when there are no appointments", async () => {
    listMock.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText(/no appointments yet/i)).toBeInTheDocument();
  });

  it("renders an appointment with the physician, reason and add-to-calendar", async () => {
    listMock.mockResolvedValue([appt({})]);
    renderPage();
    expect(await screen.findByText(/Dr\. Dana Okafor/)).toBeInTheDocument();
    expect(screen.getByText("Review throat swab")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /add to calendar/i })).toBeInTheDocument();
  });
});
