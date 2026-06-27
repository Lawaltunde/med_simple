import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { EpisodeReview } from "./EpisodeReview";
import type { Episode } from "@/features/symptom/types";

const physician = { id: "doc1", firstName: "Dana", lastName: "Okafor", role: "PHYSICIAN" };
vi.mock("@/auth/AuthProvider", () => ({ useAuth: () => ({ user: physician }) }));

// A photo submission and a voice submission — the physician must see the actual media, not just
// the AI's text reading of it.
const episode: Episode = {
  id: "ep1",
  patientId: "pat1",
  assignedClinicId: "clinic1",
  assignedPhysicianId: null,
  status: "AI_ANALYZED",
  aiUrgencyLevel: "ROUTINE",
  aiSummary: "Rash on forearm.",
  aiAnalyzedAt: "2026-06-17T12:00:00Z",
  confirmedUrgencyLevel: null,
  createdAt: "2026-06-17T11:00:00Z",
  updatedAt: "2026-06-17T12:00:00Z",
  submissions: [
    {
      id: "photo1", inputType: "PHOTO", textContent: null, mediaUrl: "/api/v1/media/m-photo",
      transcription: null, aiAnalysisResult: "Erythematous rash on the left forearm.",
      aiAnalysisRelevance: "NO_SYMPTOM",
      processingStatus: "COMPLETED", submittedAt: "2026-06-17T11:00:00Z",
    },
    {
      id: "voice1", inputType: "VOICE", textContent: null, mediaUrl: "/api/v1/media/m-voice",
      transcription: "It started itching yesterday.", aiAnalysisResult: null,
      aiAnalysisRelevance: null,
      processingStatus: "COMPLETED", submittedAt: "2026-06-17T11:05:00Z",
    },
  ],
};

vi.mock("@/features/symptom/hooks", () => ({
  useEpisode: () => ({ data: episode, isLoading: false, isError: false }),
}));
vi.mock("@/features/clinician/hooks", () => ({
  useConfirmUrgency: () => ({ mutate: vi.fn(), isPending: false, error: null }),
  useAssignToMe: () => ({ mutate: vi.fn(), isPending: false, error: null }),
}));
vi.mock("@/features/careplan/hooks", () => ({
  usePatientCarePlans: () => ({ data: [], isLoading: false }),
  useCreateCarePlan: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateCarePlan: () => ({ mutate: vi.fn(), isPending: false }),
  useEpisodeSuggestion: () => ({ data: undefined }),
  useApproveSuggestion: () => ({ mutate: vi.fn(), isPending: false }),
  useRejectSuggestion: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock("@/features/prescription/PrescriptionCard", () => ({ PrescriptionCard: () => null }));
vi.mock("@/features/diagnostic/DiagnosticCard", () => ({ DiagnosticCard: () => null }));
vi.mock("@/features/referral/ReferralCard", () => ({ ReferralCard: () => null }));
vi.mock("@/features/wearable/WearableSummaryCard", () => ({ WearableSummaryCard: () => null }));
vi.mock("@/features/appointment/BookAppointmentCard", () => ({ BookAppointmentCard: () => null }));

beforeEach(() => {
  // AuthedMedia fetches the bytes with the Bearer token; stand in a tiny blob per media URL.
  vi.stubGlobal("fetch", vi.fn((url: string) =>
    Promise.resolve({
      ok: true,
      blob: () => Promise.resolve(new Blob([new Uint8Array([1, 2, 3])], {
        type: url.includes("voice") ? "audio/webm" : "image/png",
      })),
    } as unknown as Response)));
});
afterEach(() => vi.unstubAllGlobals());

function renderReview() {
  return render(
    <MemoryRouter initialEntries={["/console/episodes/ep1"]}>
      <Routes>
        <Route path="/console/episodes/:id" element={<EpisodeReview />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("EpisodeReview media", () => {
  it("renders the actual photo and voice player alongside the AI text", async () => {
    renderReview();

    // AI-derived text is still shown for context.
    expect(screen.getByText(/erythematous rash on the left forearm/i)).toBeInTheDocument();
    expect(screen.getByText(/it started itching yesterday/i)).toBeInTheDocument();

    // The real media loads (fetched with the token, turned into an object URL).
    const img = await screen.findByAltText(/submitted symptom photo/i);
    expect(img).toBeInTheDocument();

    // Voice note renders an audio player.
    await waitFor(() =>
      expect(document.querySelector("audio")).toBeInTheDocument());

    // Both media URLs were fetched.
    const fetchMock = vi.mocked(fetch);
    const urls = fetchMock.mock.calls.map((c) => c[0]);
    expect(urls).toContain("/api/v1/media/m-photo");
    expect(urls).toContain("/api/v1/media/m-voice");
  });

  it("flags a photo the AI saw no symptom in, so the reading isn't read as confirmed", () => {
    renderReview();
    expect(screen.getByText(/no visible symptom detected in this image/i)).toBeInTheDocument();
  });
});
