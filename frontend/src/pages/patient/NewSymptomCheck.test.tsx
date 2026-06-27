import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { NewSymptomCheck } from "./NewSymptomCheck";

// Authenticated patient.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

// Stub the network at the api layer: each composed flow resolves to a triaged episode. The
// hooks (useSubmitSymptomCheck / useSubmitMediaSymptomCheck) run for real over these stubs.
const textMock = vi.fn();
const mediaMock = vi.fn();
const triageMock = vi.fn();
vi.mock("@/features/symptom/api", () => ({
  submitTextSymptomCheck: (...args: unknown[]) => textMock(...args),
  submitMediaSymptomCheck: (...args: unknown[]) => mediaMock(...args),
  triageEpisode: (...args: unknown[]) => triageMock(...args),
}));

// Capture navigation; keep the rest of react-router-dom real (MemoryRouter, useSearchParams).
const navigateMock = vi.fn();
vi.mock("react-router-dom", async (orig) => ({
  ...(await orig<typeof import("react-router-dom")>()),
  useNavigate: () => navigateMock,
}));

// The media controls have their own suites; stub them to a "set a blob" button so the
// mode-switch + submit wiring is what's under test here.
vi.mock("@/features/media/PhotoCapture", () => ({
  PhotoCapture: ({ onChange }: { onChange: (f: File | null) => void }) => (
    <button type="button" onClick={() => onChange(new File(["x"], "p.png", { type: "image/png" }))}>
      pick-photo
    </button>
  ),
}));
vi.mock("@/features/media/VoiceRecorder", () => ({
  VoiceRecorder: ({ onChange }: { onChange: (b: Blob | null) => void }) => (
    <button type="button" onClick={() => onChange(new Blob(["x"], { type: "audio/webm" }))}>
      record-voice
    </button>
  ),
}));

function renderScreen(path = "/app/new") {
  const qc = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[path]}>
        <NewSymptomCheck />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("NewSymptomCheck", () => {
  beforeEach(() => {
    textMock.mockReset();
    mediaMock.mockReset();
    triageMock.mockReset();
    navigateMock.mockReset();
  });
  afterEach(() => vi.clearAllMocks());

  // ── text mode (existing behavior) ──────────────────────────────────────────────

  it("blocks submission when the description is too short", async () => {
    const user = userEvent.setup();
    renderScreen();

    await user.type(screen.getByLabelText("What's going on?"), "sore");
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    expect(await screen.findByText(/at least 10 characters/i)).toBeInTheDocument();
    expect(textMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it("submits a valid description and navigates to the triaged episode", async () => {
    textMock.mockResolvedValueOnce({ id: "ep-42", patientId: "patient-1" });
    const user = userEvent.setup();
    renderScreen();

    await user.type(screen.getByLabelText("What's going on?"), "sore throat and fever for two days");
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    await waitFor(() =>
      expect(textMock).toHaveBeenCalledWith({
        patientId: "patient-1",
        description: "sore throat and fever for two days",
      })
    );
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith("/app/episodes/ep-42"));
    expect(mediaMock).not.toHaveBeenCalled();
  });

  it("surfaces an error message when triage fails", async () => {
    textMock.mockRejectedValueOnce(new Error("network down"));
    const user = userEvent.setup();
    renderScreen();

    await user.type(screen.getByLabelText("What's going on?"), "persistent cough for a week");
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  // ── photo + voice modes ────────────────────────────────────────────────────────

  it("opens directly in photo mode from a deep link and submits a photo check", async () => {
    mediaMock.mockResolvedValueOnce({ kind: "triaged", episode: { id: "ep-7", patientId: "patient-1" } });
    const user = userEvent.setup();
    renderScreen("/app/new?mode=PHOTO");

    // Submit is disabled until a photo is chosen.
    const submit = screen.getByRole("button", { name: /submit for triage/i });
    expect(submit).toBeDisabled();

    await user.click(screen.getByRole("button", { name: /pick-photo/i }));
    expect(submit).toBeEnabled();
    await user.click(submit);

    await waitFor(() => expect(mediaMock).toHaveBeenCalledTimes(1));
    expect(mediaMock.mock.calls[0][0]).toMatchObject({ patientId: "patient-1", inputType: "PHOTO" });
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith("/app/episodes/ep-7"));
    expect(textMock).not.toHaveBeenCalled();
  });

  it("switches to voice mode via the tab and submits a voice check", async () => {
    mediaMock.mockResolvedValueOnce({ kind: "triaged", episode: { id: "ep-9", patientId: "patient-1" } });
    const user = userEvent.setup();
    renderScreen();

    await user.click(screen.getByRole("tab", { name: /voice/i }));
    await user.click(screen.getByRole("button", { name: /record-voice/i }));
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    await waitFor(() => expect(mediaMock).toHaveBeenCalledTimes(1));
    expect(mediaMock.mock.calls[0][0]).toMatchObject({ inputType: "VOICE" });
  });

  // ── photo with no visible symptom: prompt, don't auto-triage ────────────────────

  it("prompts for review instead of navigating when the photo shows no symptom", async () => {
    mediaMock.mockResolvedValueOnce({
      kind: "needs_review",
      episodeId: "ep-nr",
      description: "This looks like a photo of a dog, not a symptom.",
    });
    const user = userEvent.setup();
    renderScreen("/app/new?mode=PHOTO");

    await user.click(screen.getByRole("button", { name: /pick-photo/i }));
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    // No navigation yet — the patient is shown the choice.
    expect(await screen.findByText(/couldn't see a clear symptom/i)).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
    expect(triageMock).not.toHaveBeenCalled();
  });

  it("sends the photo for review anyway, triaging the held episode", async () => {
    mediaMock.mockResolvedValueOnce({
      kind: "needs_review",
      episodeId: "ep-nr",
      description: "No visible symptom.",
    });
    triageMock.mockResolvedValueOnce({ id: "ep-nr", patientId: "patient-1" });
    const user = userEvent.setup();
    renderScreen("/app/new?mode=PHOTO");

    await user.click(screen.getByRole("button", { name: /pick-photo/i }));
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));
    await user.click(await screen.findByRole("button", { name: /send for review anyway/i }));

    await waitFor(() => expect(triageMock).toHaveBeenCalledWith("ep-nr"));
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith("/app/episodes/ep-nr"));
  });

  it("lets the patient choose a different photo, dismissing the prompt without triaging", async () => {
    mediaMock.mockResolvedValueOnce({
      kind: "needs_review",
      episodeId: "ep-nr",
      description: "No visible symptom.",
    });
    const user = userEvent.setup();
    renderScreen("/app/new?mode=PHOTO");

    await user.click(screen.getByRole("button", { name: /pick-photo/i }));
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));
    await user.click(await screen.findByRole("button", { name: /choose a different photo/i }));

    // Back to the picker; nothing triaged, no navigation.
    expect(screen.getByRole("button", { name: /pick-photo/i })).toBeInTheDocument();
    expect(triageMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
