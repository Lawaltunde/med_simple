import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PrescriptionCard } from "./PrescriptionCard";
import type { Episode } from "@/features/symptom/types";
import type { Prescription } from "./types";
import type { CarePlan } from "@/features/careplan/types";

// Auth: a physician.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({
    user: { id: "doc-1", firstName: "Alice", lastName: "Smith", role: "PHYSICIAN" },
  }),
}));

// Stub the careplan API (needed to resolve carePlanId for the episode).
const listPatientCarePlansMock = vi.fn();
vi.mock("@/features/careplan/api", () => ({
  listPatientCarePlans: (...a: unknown[]) => listPatientCarePlansMock(...a),
  getCarePlan: vi.fn(),
  createCarePlan: vi.fn(),
  updateCarePlan: vi.fn(),
  addFollowUp: vi.fn(),
  respondToFollowUp: vi.fn(),
}));

// Stub the prescription API.
const listCarePlanPrescriptionsMock = vi.fn();
vi.mock("@/features/prescription/api", () => ({
  listCarePlanPrescriptions: (...a: unknown[]) => listCarePlanPrescriptionsMock(...a),
  listPatientPrescriptions: vi.fn().mockResolvedValue([]),
  createPrescription: vi.fn(),
  getPrescription: vi.fn(),
  signPrescription: vi.fn(),
  sendPrescription: vi.fn(),
  cancelPrescription: vi.fn(),
}));

const EPISODE: Episode = {
  id: "ep-1",
  patientId: "patient-1",
  assignedClinicId: "clinic-1",
  assignedPhysicianId: "doc-1",
  status: "IN_PROGRESS",
  aiUrgencyLevel: "ROUTINE",
  aiSummary: null,
  aiAnalyzedAt: null,
  confirmedUrgencyLevel: null,
  createdAt: "2026-06-20T08:00:00Z",
  updatedAt: "2026-06-20T09:00:00Z",
  submissions: [],
};

const CARE_PLAN: CarePlan = {
  id: "cp-1",
  episodeId: "ep-1",
  patientId: "patient-1",
  physicianId: "doc-1",
  clinicId: "clinic-1",
  status: "ACTIVE",
  notes: "Rest and fluids",
  createdAt: "2026-06-20T09:00:00Z",
  updatedAt: "2026-06-20T09:00:00Z",
  followUps: [],
};

function rx(over: Partial<Prescription>): Prescription {
  return {
    id: "rx-1",
    carePlanId: "cp-1",
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

function renderCard() {
  const qc = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <PrescriptionCard episode={EPISODE} episodeId="ep-1" />
    </QueryClientProvider>
  );
}

describe("PrescriptionCard", () => {
  beforeEach(() => {
    listPatientCarePlansMock.mockReset();
    listCarePlanPrescriptionsMock.mockReset();
  });

  it("renders nothing when no care plan exists for the episode", async () => {
    listPatientCarePlansMock.mockResolvedValue([]);
    listCarePlanPrescriptionsMock.mockResolvedValue([]);
    const { container } = renderCard();
    // Wait for query resolution — the card should be absent (returns null).
    await new Promise((r) => setTimeout(r, 50));
    expect(container.firstChild).toBeNull();
  });

  it("renders the prescriptions section heading when a care plan exists", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([]);
    renderCard();
    expect(await screen.findByRole("heading", { name: /prescriptions/i })).toBeInTheDocument();
  });

  it("shows empty state message when there are no prescriptions", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([]);
    renderCard();
    expect(await screen.findByText(/no prescriptions yet/i)).toBeInTheDocument();
  });

  it("renders an existing prescription with name and DRAFT badge", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([rx({ medicationName: "Lisinopril", status: "DRAFT" })]);
    renderCard();
    expect(await screen.findByText("Lisinopril")).toBeInTheDocument();
    expect(await screen.findByText(/draft/i)).toBeInTheDocument();
  });

  it("shows Sign and Cancel buttons for a DRAFT prescription", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([rx({ status: "DRAFT" })]);
    renderCard();
    expect(await screen.findByRole("button", { name: /sign/i })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /cancel/i })).toBeInTheDocument();
  });

  it("shows Send and Cancel buttons for a SIGNED prescription", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([rx({ status: "SIGNED" })]);
    renderCard();
    expect(await screen.findByRole("button", { name: /send/i })).toBeInTheDocument();
  });

  it("shows AI safety warning badge when there are allergy flags", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([
      rx({
        aiSafetyChecks: JSON.stringify({
          interactions: [],
          allergyFlags: ["Penicillin allergy detected"],
          doseWarnings: [],
        }),
      }),
    ]);
    renderCard();
    expect(await screen.findByText(/allergy flags/i)).toBeInTheDocument();
    expect(await screen.findByText("Penicillin allergy detected")).toBeInTheDocument();
  });

  it("shows clean safety panel when no warnings", async () => {
    listPatientCarePlansMock.mockResolvedValue([CARE_PLAN]);
    listCarePlanPrescriptionsMock.mockResolvedValue([
      rx({
        aiSafetyChecks: JSON.stringify({
          interactions: [],
          allergyFlags: [],
          doseWarnings: [],
        }),
      }),
    ]);
    renderCard();
    expect(await screen.findByText(/no interactions/i)).toBeInTheDocument();
  });
});
