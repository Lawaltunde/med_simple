import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { EpisodeReview } from "./EpisodeReview";
import type { Episode } from "@/features/symptom/types";

const physician = { id: "doc1", firstName: "Dana", lastName: "Okafor", role: "PHYSICIAN" };
vi.mock("@/auth/AuthProvider", () => ({ useAuth: () => ({ user: physician }) }));

const episode: Episode = {
  id: "ep1",
  patientId: "pat1",
  assignedClinicId: "clinic1",
  assignedPhysicianId: null,
  status: "AI_ANALYZED",
  aiUrgencyLevel: "ROUTINE",
  aiSummary: "Sore throat, low fever, two days.",
  aiAnalyzedAt: "2026-06-17T12:00:00Z",
  confirmedUrgencyLevel: null,
  createdAt: "2026-06-17T11:00:00Z",
  updatedAt: "2026-06-17T12:00:00Z",
  submissions: [
    {
      id: "s1", inputType: "TEXT", textContent: "throat hurts", mediaUrl: null,
      transcription: null, aiAnalysisResult: null, aiAnalysisRelevance: null, processingStatus: "COMPLETED",
      submittedAt: "2026-06-17T11:00:00Z",
    },
  ],
};

vi.mock("@/features/symptom/hooks", () => ({
  useEpisode: () => ({ data: episode, isLoading: false, isError: false }),
}));

const confirmMutate = vi.fn();
const assignMutate = vi.fn();
vi.mock("@/features/clinician/hooks", () => ({
  useConfirmUrgency: () => ({ mutate: confirmMutate, isPending: false, error: null }),
  useAssignToMe: () => ({ mutate: assignMutate, isPending: false, error: null }),
}));

// Care-plan card is out of scope here; stub its hooks (no plan yet -> authoring form,
// no AI suggestion -> blank create form).
vi.mock("@/features/careplan/hooks", () => ({
  usePatientCarePlans: () => ({ data: [], isLoading: false }),
  useCreateCarePlan: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateCarePlan: () => ({ mutate: vi.fn(), isPending: false }),
  useEpisodeSuggestion: () => ({ data: undefined }),
  useApproveSuggestion: () => ({ mutate: vi.fn(), isPending: false }),
  useRejectSuggestion: () => ({ mutate: vi.fn(), isPending: false }),
}));

// The prescription/diagnostic/referral cards have their own test suites and pull in
// their own React Query hooks. This test covers the console's own logic (assign /
// confirm / care plan), so stub the feature cards to null to keep it isolated.
vi.mock("@/features/prescription/PrescriptionCard", () => ({ PrescriptionCard: () => null }));
vi.mock("@/features/diagnostic/DiagnosticCard", () => ({ DiagnosticCard: () => null }));
vi.mock("@/features/referral/ReferralCard", () => ({ ReferralCard: () => null }));
vi.mock("@/features/wearable/WearableSummaryCard", () => ({ WearableSummaryCard: () => null }));
vi.mock("@/features/appointment/BookAppointmentCard", () => ({ BookAppointmentCard: () => null }));

function renderReview() {
  return render(
    <MemoryRouter initialEntries={["/console/episodes/ep1"]}>
      <Routes>
        <Route path="/console/episodes/:id" element={<EpisodeReview />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("EpisodeReview", () => {
  beforeEach(() => {
    confirmMutate.mockReset();
    assignMutate.mockReset();
  });

  it("requires an override reason before an override can be confirmed", async () => {
    const user = userEvent.setup();
    renderReview();

    // Default matches the AI level (ROUTINE) -> confirm is allowed straight away.
    expect(screen.getByRole("button", { name: /confirm urgency/i })).toBeEnabled();

    // Override to URGENT -> reason now required, action blocked until it's filled.
    await user.click(screen.getByRole("button", { name: "Urgent" }));
    const overrideBtn = screen.getByRole("button", { name: /override urgency/i });
    expect(overrideBtn).toBeDisabled();
    expect(screen.getByLabelText("Reason for override")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Reason for override"), "Red flags the AI missed");
    expect(overrideBtn).toBeEnabled();

    await user.click(overrideBtn);
    expect(confirmMutate).toHaveBeenCalledWith({
      episodeId: "ep1",
      physicianId: "doc1",
      confirmedUrgencyLevel: "URGENT",
      overrideReason: "Red flags the AI missed",
    });
  });

  it("assigns the episode to the logged-in physician", async () => {
    const user = userEvent.setup();
    renderReview();

    await user.click(screen.getByRole("button", { name: /assign to me/i }));
    expect(assignMutate).toHaveBeenCalledWith({ episodeId: "ep1", physicianId: "doc1" });
  });

  it("confirming the AI level outright sends no override reason", async () => {
    const user = userEvent.setup();
    renderReview();

    await user.click(screen.getByRole("button", { name: /confirm urgency/i }));
    expect(confirmMutate).toHaveBeenCalledWith({
      episodeId: "ep1",
      physicianId: "doc1",
      confirmedUrgencyLevel: "ROUTINE",
      overrideReason: undefined,
    });
  });
});
