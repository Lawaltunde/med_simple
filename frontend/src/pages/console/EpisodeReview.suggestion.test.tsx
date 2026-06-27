import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { EpisodeReview } from "./EpisodeReview";
import type { Episode } from "@/features/symptom/types";

const physician = { id: "doc1", firstName: "Dana", lastName: "Okafor", role: "PHYSICIAN" };
vi.mock("@/auth/AuthProvider", () => ({ useAuth: () => ({ user: physician }) }));

const episode: Episode = {
  id: "ep1", patientId: "pat1", assignedClinicId: "clinic1", assignedPhysicianId: "doc1",
  status: "ASSIGNED", aiUrgencyLevel: "SEE_SOON", aiSummary: "Sore throat.",
  aiAnalyzedAt: "2026-06-23T12:00:00Z", confirmedUrgencyLevel: null,
  createdAt: "2026-06-23T11:00:00Z", updatedAt: "2026-06-23T12:00:00Z",
  submissions: [{
    id: "s1", inputType: "TEXT", textContent: "throat hurts", mediaUrl: null,
    transcription: null, aiAnalysisResult: null, aiAnalysisRelevance: null, processingStatus: "COMPLETED",
    submittedAt: "2026-06-23T11:00:00Z",
  }],
};

vi.mock("@/features/symptom/hooks", () => ({
  useEpisode: () => ({ data: episode, isLoading: false, isError: false }),
}));
vi.mock("@/features/clinician/hooks", () => ({
  useConfirmUrgency: () => ({ mutate: vi.fn(), isPending: false, error: null }),
  useAssignToMe: () => ({ mutate: vi.fn(), isPending: false, error: null }),
}));
// Isolate the console from the other feature cards.
vi.mock("@/features/prescription/PrescriptionCard", () => ({ PrescriptionCard: () => null }));
vi.mock("@/features/diagnostic/DiagnosticCard", () => ({ DiagnosticCard: () => null }));
vi.mock("@/features/referral/ReferralCard", () => ({ ReferralCard: () => null }));
vi.mock("@/features/wearable/WearableSummaryCard", () => ({ WearableSummaryCard: () => null }));
vi.mock("@/features/appointment/BookAppointmentCard", () => ({ BookAppointmentCard: () => null }));

const approveMutate = vi.fn();
const rejectMutate = vi.fn();
const PROPOSED = {
  id: "sg1", episodeId: "ep1", status: "PROPOSED",
  suggestedPlan: "Likely viral pharyngitis. Supportive care: rest, fluids, analgesia.",
  rationale: "Symptoms and duration fit a viral course.",
  adapter: "openai", model: "gpt-4o", createdAt: "2026-06-23T12:00:00Z",
};

vi.mock("@/features/careplan/hooks", () => ({
  usePatientCarePlans: () => ({ data: [], isLoading: false }), // no plan yet -> authoring
  useCreateCarePlan: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateCarePlan: () => ({ mutate: vi.fn(), isPending: false }),
  useEpisodeSuggestion: () => ({ data: PROPOSED }),
  useApproveSuggestion: () => ({ mutate: approveMutate, isPending: false }),
  useRejectSuggestion: () => ({ mutate: rejectMutate, isPending: false }),
}));

function renderReview() {
  return render(
    <MemoryRouter initialEntries={["/console/episodes/ep1"]}>
      <Routes>
        <Route path="/console/episodes/:id" element={<EpisodeReview />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("EpisodeReview — AI care suggestion", () => {
  beforeEach(() => {
    approveMutate.mockReset();
    rejectMutate.mockReset();
  });

  it("shows the AI draft with its disclaimer and pre-fills the editor", async () => {
    renderReview();
    expect(await screen.findByText(/AI suggested care plan/i)).toBeInTheDocument();
    expect(screen.getByText(/Advisory only/i)).toBeInTheDocument();
    expect(screen.getByText(/Symptoms and duration fit a viral course/i)).toBeInTheDocument();
    // Editor pre-filled with the suggested plan.
    expect(screen.getByDisplayValue(/Likely viral pharyngitis/i)).toBeInTheDocument();
  });

  it("approves the suggestion unchanged (no editedNotes)", async () => {
    const user = userEvent.setup();
    renderReview();
    await user.click(await screen.findByRole("button", { name: /approve & send/i }));
    await waitFor(() =>
      expect(approveMutate).toHaveBeenCalledWith({ id: "sg1", physicianId: "doc1", editedNotes: undefined })
    );
  });

  it("sends editedNotes when the physician changes the text", async () => {
    const user = userEvent.setup();
    renderReview();
    const editor = await screen.findByDisplayValue(/Likely viral pharyngitis/i);
    await user.clear(editor);
    await user.type(editor, "My revised plan: rest and reassess in 3 days.");
    await user.click(screen.getByRole("button", { name: /approve & send/i }));
    await waitFor(() =>
      expect(approveMutate).toHaveBeenCalledWith({
        id: "sg1",
        physicianId: "doc1",
        editedNotes: "My revised plan: rest and reassess in 3 days.",
      })
    );
  });

  it("rejects the suggestion", async () => {
    const user = userEvent.setup();
    renderReview();
    await user.click(await screen.findByRole("button", { name: /reject, write my own/i }));
    expect(rejectMutate).toHaveBeenCalled();
    expect(rejectMutate.mock.calls[0][0]).toMatchObject({ id: "sg1", physicianId: "doc1" });
  });
});
