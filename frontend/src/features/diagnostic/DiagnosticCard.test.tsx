import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DiagnosticCard } from "./DiagnosticCard";
import type { Episode } from "@/features/symptom/types";
import type { Requisition } from "./types";

// Authenticated physician.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({
    user: { id: "doc-1", firstName: "Dr", lastName: "Smith", role: "PHYSICIAN" },
  }),
}));

const getRequisitionsMock = vi.fn();
const createRequisitionMock = vi.fn();
const sendRequisitionMock = vi.fn();
const cancelRequisitionMock = vi.fn();
const recordResultMock = vi.fn();
const addInterpretationMock = vi.fn();
const releaseResultMock = vi.fn();
const getResultMock = vi.fn();

vi.mock("@/features/diagnostic/api", () => ({
  getEpisodeRequisitions: (...a: unknown[]) => getRequisitionsMock(...a),
  createRequisition: (...a: unknown[]) => createRequisitionMock(...a),
  sendRequisition: (...a: unknown[]) => sendRequisitionMock(...a),
  cancelRequisition: (...a: unknown[]) => cancelRequisitionMock(...a),
  recordResult: (...a: unknown[]) => recordResultMock(...a),
  addInterpretation: (...a: unknown[]) => addInterpretationMock(...a),
  releaseResult: (...a: unknown[]) => releaseResultMock(...a),
  getResultForPhysician: (...a: unknown[]) => getResultMock(...a),
  listPatientResults: vi.fn(),
}));

// AI care suggestion: mock the hook directly (matches BookAppointmentCard.test).
const suggestionData = vi.fn<() => unknown>(() => undefined);
vi.mock("@/features/careplan/hooks", () => ({
  useEpisodeSuggestion: () => ({ data: suggestionData() }),
}));

function episode(over: Partial<Episode> = {}): Episode {
  return {
    id: "ep-1",
    patientId: "patient-1",
    assignedClinicId: "clinic-1",
    assignedPhysicianId: "doc-1",
    status: "IN_PROGRESS",
    aiUrgencyLevel: "ROUTINE",
    aiSummary: "Mild symptoms.",
    aiAnalyzedAt: "2026-06-20T09:00:00Z",
    confirmedUrgencyLevel: "ROUTINE",
    createdAt: "2026-06-20T08:00:00Z",
    updatedAt: "2026-06-20T09:00:00Z",
    submissions: [],
    ...over,
  };
}

function requisition(over: Partial<Requisition> = {}): Requisition {
  return {
    id: "req-1",
    episodeId: "ep-1",
    carePlanId: null,
    patientId: "patient-1",
    orderingPhysicianId: "doc-1",
    testType: "Complete Blood Count",
    testCodes: ["58410-2"],
    clinicalIndication: "Anaemia screen",
    urgency: "ROUTINE",
    status: "DRAFT",
    deliveryMethod: null,
    diagnosticCentreId: null,
    documentUrl: null,
    appointmentAt: null,
    sentAt: null,
    bookedAt: null,
    createdAt: "2026-06-20T10:00:00Z",
    ...over,
  };
}

function renderCard(ep = episode()) {
  const qc = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <DiagnosticCard episode={ep} episodeId={ep.id} />
    </QueryClientProvider>
  );
}

describe("DiagnosticCard", () => {
  beforeEach(() => {
    getRequisitionsMock.mockReset();
    createRequisitionMock.mockReset();
    sendRequisitionMock.mockReset();
    suggestionData.mockReset();
    suggestionData.mockReturnValue(undefined);
  });

  function suggestion(over: Record<string, unknown> = {}) {
    return {
      id: "sug-1",
      episodeId: "ep-1",
      status: "PROPOSED",
      suggestedPlan: "Check thyroid",
      rationale: "fatigue",
      suggestedFollowUpDays: null,
      suggestedReferralSpecialty: null,
      suggestedTests: ["TSH", "CBC"],
      adapter: "openai",
      model: "gpt-4o",
      createdAt: "2026-06-20T10:00:00Z",
      ...over,
    };
  }

  it("shows the AI suggested-tests banner with a Use button per test", async () => {
    getRequisitionsMock.mockResolvedValue([]);
    suggestionData.mockReturnValue(suggestion({ suggestedTests: ["TSH", "CBC"] }));

    renderCard();

    expect(await screen.findByText(/AI suggests: TSH, CBC/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /use TSH/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /use CBC/i })).toBeInTheDocument();
  });

  it("prefills the test-type field and opens the form on 'Use'", async () => {
    getRequisitionsMock.mockResolvedValue([]);
    suggestionData.mockReturnValue(suggestion({ suggestedTests: ["TSH"] }));

    const user = userEvent.setup();
    renderCard();

    await user.click(await screen.findByRole("button", { name: /use TSH/i }));

    const testTypeInput = await screen.findByPlaceholderText(/complete blood count/i);
    expect(testTypeInput).toHaveValue("TSH");
  });

  it("does not show the AI banner when there are no suggested tests", async () => {
    getRequisitionsMock.mockResolvedValue([]);
    suggestionData.mockReturnValue(suggestion({ suggestedTests: [] }));

    renderCard();

    expect(await screen.findByRole("button", { name: /order new test/i })).toBeInTheDocument();
    expect(screen.queryByText(/AI suggests:/i)).not.toBeInTheDocument();
  });

  it("renders the diagnostics heading and order form without needing a care plan", async () => {
    getRequisitionsMock.mockResolvedValue([]);
    renderCard();
    expect(await screen.findByText(/diagnostics/i)).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /order new test/i })).toBeInTheDocument();
  });

  it("shows existing requisitions with their test type", async () => {
    getRequisitionsMock.mockResolvedValue([requisition({ testType: "Lipid Panel" })]);
    renderCard();
    expect(await screen.findByText("Lipid Panel")).toBeInTheDocument();
  });

  it("opens the create form and submits a new requisition against the episode", async () => {
    getRequisitionsMock.mockResolvedValue([]);
    createRequisitionMock.mockResolvedValue(requisition());
    const user = userEvent.setup();
    renderCard();

    await user.click(await screen.findByRole("button", { name: /order new test/i }));
    const testTypeInput = await screen.findByPlaceholderText(/complete blood count/i);
    await user.type(testTypeInput, "Thyroid Panel");
    await user.click(screen.getByRole("button", { name: /create requisition/i }));

    expect(createRequisitionMock).toHaveBeenCalledWith(
      expect.objectContaining({ episodeId: "ep-1", testType: "Thyroid Panel", urgency: "ROUTINE" })
    );
  });

  it("shows Send to lab and Cancel for DRAFT requisitions", async () => {
    getRequisitionsMock.mockResolvedValue([requisition({ status: "DRAFT" })]);
    const user = userEvent.setup();
    renderCard();

    await user.click(await screen.findByText("Complete Blood Count"));
    expect(await screen.findByRole("button", { name: /send to lab/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
  });

  it("does not crash for the physician role", () => {
    getRequisitionsMock.mockResolvedValue([]);
    const { container } = renderCard();
    expect(container).toBeDefined();
  });
});
