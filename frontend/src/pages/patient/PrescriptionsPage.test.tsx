import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PrescriptionsPage } from "./PrescriptionsPage";
import type { Prescription } from "@/features/prescription/types";

// Authenticated patient.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

// Stub the prescription API layer.
const listPatientMock = vi.fn();
vi.mock("@/features/prescription/api", () => ({
  listPatientPrescriptions: (...a: unknown[]) => listPatientMock(...a),
  listEpisodePrescriptions: vi.fn().mockResolvedValue([]),
  createPrescription: vi.fn(),
  getPrescription: vi.fn(),
  signPrescription: vi.fn(),
  sendPrescription: vi.fn(),
  cancelPrescription: vi.fn(),
}));

function rx(over: Partial<Prescription>): Prescription {
  return {
    id: "rx-1",
    episodeId: "ep-1",
    carePlanId: null,
    patientId: "patient-1",
    prescribingPhysicianId: "doc-1",
    medicationName: "Amoxicillin",
    medicationCode: null,
    dosage: "500 mg",
    frequency: "Three times daily",
    duration: "7 days",
    refills: 0,
    instructions: null,
    aiSafetyChecks: null,
    status: "DRAFT",
    deliveryMethod: null,
    pharmacyId: null,
    documentUrl: null,
    signedAt: null,
    sentAt: null,
    filledAt: null,
    createdAt: "2026-06-20T10:00:00Z",
    ...over,
  };
}

function renderScreen() {
  const qc = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <PrescriptionsPage />
    </QueryClientProvider>
  );
}

describe("PrescriptionsPage", () => {
  beforeEach(() => listPatientMock.mockReset());

  it("shows the empty state when there are no prescriptions", async () => {
    listPatientMock.mockResolvedValue([]);
    renderScreen();
    expect(await screen.findByText(/no prescriptions yet/i)).toBeInTheDocument();
  });

  it("renders a prescription with medication name and status badge", async () => {
    listPatientMock.mockResolvedValue([rx({ medicationName: "Metformin", status: "SIGNED" })]);
    renderScreen();
    expect(await screen.findByText("Metformin")).toBeInTheDocument();
    expect(await screen.findByText(/signed/i)).toBeInTheDocument();
  });

  it("shows dosage and frequency", async () => {
    listPatientMock.mockResolvedValue([rx({ dosage: "850 mg", frequency: "Twice daily" })]);
    renderScreen();
    expect(await screen.findByText(/850 mg/)).toBeInTheDocument();
    expect(await screen.findByText(/Twice daily/)).toBeInTheDocument();
  });

  it("renders filled status badge correctly", async () => {
    listPatientMock.mockResolvedValue([rx({ status: "FILLED", filledAt: "2026-06-21T14:00:00Z" })]);
    renderScreen();
    // Exact match targets the status badge ("Filled"), not the "Filled <date>" line.
    expect(await screen.findByText("Filled")).toBeInTheDocument();
  });

  it("passes patient id to the API call", async () => {
    listPatientMock.mockResolvedValue([]);
    renderScreen();
    await screen.findByText(/no prescriptions yet/i);
    expect(listPatientMock).toHaveBeenCalledWith("patient-1");
  });

  it("renders multiple prescriptions", async () => {
    listPatientMock.mockResolvedValue([
      rx({ id: "rx-1", medicationName: "Drug A" }),
      rx({ id: "rx-2", medicationName: "Drug B" }),
    ]);
    renderScreen();
    expect(await screen.findByText("Drug A")).toBeInTheDocument();
    expect(await screen.findByText("Drug B")).toBeInTheDocument();
  });
});
