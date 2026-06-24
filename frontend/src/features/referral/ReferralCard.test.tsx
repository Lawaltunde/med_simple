import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReferralCard } from "./ReferralCard";
import type { Episode } from "@/features/symptom/types";
import type { CarePlan } from "@/features/careplan/types";
import type { Referral } from "./types";

// ── Auth mocks ─────────────────────────────────────────────────────────────────

const mockRole = vi.fn<() => string>(() => "PHYSICIAN");
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "doc-1", role: mockRole() } }),
}));

// ── CarePlan network layer ─────────────────────────────────────────────────────

const listCarePlansMock = vi.fn();
vi.mock("@/features/careplan/api", () => ({
  listPatientCarePlans: (...a: unknown[]) => listCarePlansMock(...a),
  getCarePlan: vi.fn(),
  createCarePlan: vi.fn(),
  updateCarePlan: vi.fn(),
  addFollowUp: vi.fn(),
  respondToFollowUp: vi.fn(),
}));

// ── Referral network layer ─────────────────────────────────────────────────────

const getEpisodeReferralsMock = vi.fn();
const createReferralMock = vi.fn();
const confirmSpecialtyMock = vi.fn();
const submitReferralMock = vi.fn();

vi.mock("@/features/referral/api", () => ({
  getEpisodeReferrals: (...a: unknown[]) => getEpisodeReferralsMock(...a),
  createReferral: (...a: unknown[]) => createReferralMock(...a),
  confirmSpecialty: (...a: unknown[]) => confirmSpecialtyMock(...a),
  submitReferral: (...a: unknown[]) => submitReferralMock(...a),
  cancelReferral: vi.fn(),
  getPatientReferrals: vi.fn().mockResolvedValue([]),
  getReferral: vi.fn(),
}));

// ── Test fixtures ──────────────────────────────────────────────────────────────

function episode(over: Partial<Episode> = {}): Episode {
  return {
    id: "ep-1",
    patientId: "pat-1",
    assignedClinicId: "clinic-1",
    assignedPhysicianId: "doc-1",
    status: "IN_PROGRESS",
    aiUrgencyLevel: "ROUTINE",
    aiSummary: "Viral illness",
    aiAnalyzedAt: "2026-06-01T10:00:00Z",
    confirmedUrgencyLevel: "ROUTINE",
    createdAt: "2026-06-01T09:00:00Z",
    updatedAt: "2026-06-01T11:00:00Z",
    submissions: [],
    ...over,
  };
}

function carePlan(over: Partial<CarePlan> = {}): CarePlan {
  return {
    id: "cp-1",
    episodeId: "ep-1",
    patientId: "pat-1",
    physicianId: "doc-1",
    clinicId: "clinic-1",
    status: "ACTIVE",
    notes: "Rest and fluids",
    createdAt: "2026-06-01T12:00:00Z",
    updatedAt: "2026-06-01T12:00:00Z",
    followUps: [],
    ...over,
  };
}

function referral(over: Partial<Referral> = {}): Referral {
  return {
    id: "ref-1",
    episodeId: "ep-1",
    carePlanId: null,
    patientId: "pat-1",
    referringPhysicianId: "doc-1",
    aiSuggestedSpecialty: "Cardiology",
    confirmedSpecialty: null,
    clinicalSummary: "Progressive dyspnoea",
    urgency: "ROUTINE",
    status: "DRAFT",
    deliveryMethod: null,
    externalReferralId: null,
    documentUrl: null,
    submittedAt: null,
    createdAt: "2026-06-01T13:00:00Z",
    ...over,
  };
}

function renderCard(ep: Episode = episode()) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <ReferralCard episode={ep} episodeId={ep.id} />
    </QueryClientProvider>
  );
}

// ── Tests ──────────────────────────────────────────────────────────────────────

describe("ReferralCard", () => {
  beforeEach(() => {
    mockRole.mockReturnValue("PHYSICIAN");
    listCarePlansMock.mockReset();
    getEpisodeReferralsMock.mockReset();
    createReferralMock.mockReset();
    confirmSpecialtyMock.mockReset();
    submitReferralMock.mockReset();
  });

  it("renders nothing for a non-physician user", async () => {
    mockRole.mockReturnValue("PATIENT");
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([]);
    const { container } = renderCard();
    // Nothing rendered — null output
    expect(container.firstChild).toBeNull();
  });

  it("renders the referral form without needing a care plan", async () => {
    getEpisodeReferralsMock.mockResolvedValue([]);
    renderCard();
    expect(await screen.findByText(/specialist referral/i)).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /create referral/i })).toBeInTheDocument();
  });

  it("shows create form when there are no referrals yet", async () => {
    getEpisodeReferralsMock.mockResolvedValue([]);
    renderCard();
    expect(await screen.findByText(/specialist referral/i)).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /create referral/i })).toBeInTheDocument();
  });

  it("disables create button when summary is too short", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([]);
    renderCard();
    const btn = await screen.findByRole("button", { name: /create referral/i });
    expect(btn).toBeDisabled();
  });

  it("calls createReferral with correct payload on submit", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([]);
    createReferralMock.mockResolvedValue(referral());

    const user = userEvent.setup();
    renderCard();

    const textarea = await screen.findByPlaceholderText(/52-year-old/i);
    await user.type(textarea, "Patient with worsening chest pain");

    const btn = screen.getByRole("button", { name: /create referral/i });
    expect(btn).not.toBeDisabled();
    await user.click(btn);

    await waitFor(() =>
      expect(createReferralMock).toHaveBeenCalledWith({
        episodeId: "ep-1",
        clinicalSummary: "Patient with worsening chest pain",
        urgency: "ROUTINE",
      })
    );
  });

  it("shows AI suggestion and confirm button for a DRAFT referral", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([referral()]);

    renderCard();

    expect(await screen.findByText("Cardiology")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /confirm specialty/i })).toBeInTheDocument();
  });

  it("calls confirmSpecialty when physician accepts the AI suggestion", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([referral({ aiSuggestedSpecialty: "Cardiology" })]);
    confirmSpecialtyMock.mockResolvedValue(referral({ confirmedSpecialty: "Cardiology" }));

    const user = userEvent.setup();
    renderCard();

    const btn = await screen.findByRole("button", { name: /confirm specialty/i });
    await user.click(btn);

    await waitFor(() =>
      expect(confirmSpecialtyMock).toHaveBeenCalledWith("ref-1", "Cardiology")
    );
  });

  it("submit button is disabled until specialty is confirmed", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    // confirmedSpecialty is null → submit must be disabled
    getEpisodeReferralsMock.mockResolvedValue([referral({ confirmedSpecialty: null })]);

    renderCard();

    const submitBtn = await screen.findByRole("button", { name: /submit referral/i });
    expect(submitBtn).toBeDisabled();
  });

  it("calls submitReferral once the specialty is confirmed", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([
      referral({ confirmedSpecialty: "Cardiology" }),
    ]);
    submitReferralMock.mockResolvedValue(
      referral({ confirmedSpecialty: "Cardiology", status: "SUBMITTED", deliveryMethod: "OCEAN_EREFERRAL" })
    );

    const user = userEvent.setup();
    renderCard();

    const submitBtn = await screen.findByRole("button", { name: /submit referral/i });
    expect(submitBtn).not.toBeDisabled();
    await user.click(submitBtn);

    await waitFor(() => expect(submitReferralMock).toHaveBeenCalledWith("ref-1"));
  });

  it("shows delivery method and submitted status after submission", async () => {
    listCarePlansMock.mockResolvedValue([carePlan()]);
    getEpisodeReferralsMock.mockResolvedValue([
      referral({
        confirmedSpecialty: "Cardiology",
        status: "SUBMITTED",
        deliveryMethod: "OCEAN_EREFERRAL",
        submittedAt: "2026-06-01T14:00:00Z",
      }),
    ]);

    renderCard();

    expect(await screen.findByText("Submitted")).toBeInTheDocument();
    expect(screen.getByText("Ocean eReferral")).toBeInTheDocument();
  });
});
