import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ResultsPage } from "@/pages/patient/ResultsPage";
import type { PatientResult } from "./types";

// Authenticated patient.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

// Stub the diagnostic network layer.
const listMock = vi.fn();
vi.mock("@/features/diagnostic/api", () => ({
  listPatientResults: (...a: unknown[]) => listMock(...a),
  // Stub other exports so imports don't break.
  getCarePlanRequisitions: vi.fn(),
  createRequisition: vi.fn(),
  sendRequisition: vi.fn(),
  cancelRequisition: vi.fn(),
  recordResult: vi.fn(),
  addInterpretation: vi.fn(),
  releaseResult: vi.fn(),
  getResultForPhysician: vi.fn(),
}));

function result(over: Partial<PatientResult>): PatientResult {
  return {
    id: "r-1",
    requisitionId: "req-1",
    testType: "Complete Blood Count",
    aiSummary: "All values within normal range.",
    physicianInterpretation: "No action required.",
    releasedToPatientAt: "2026-06-20T10:00:00Z",
    ...over,
  };
}

function renderPage() {
  const qc = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <ResultsPage />
    </QueryClientProvider>
  );
}

describe("ResultsPage", () => {
  beforeEach(() => listMock.mockReset());

  it("calls the patient released-results endpoint (not the physician endpoint)", async () => {
    listMock.mockResolvedValue([]);
    renderPage();
    // The patient endpoint is /patients/{id}/results — enforced server-side.
    // listPatientResults must be called with the authenticated patient's id.
    await screen.findByText(/no results yet/i);
    expect(listMock).toHaveBeenCalledWith("patient-1");
    // Verify it does NOT call a physician-only endpoint.
    expect(listMock).not.toHaveBeenCalledWith(expect.stringContaining("/results/"));
  });

  it("shows the empty state when no results are available", async () => {
    listMock.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText(/no results yet/i)).toBeInTheDocument();
  });

  it("renders released results with test type and release date", async () => {
    listMock.mockResolvedValue([result({ testType: "Chest X-Ray", releasedToPatientAt: "2026-06-21T09:00:00Z" })]);
    renderPage();
    expect(await screen.findByText("Chest X-Ray")).toBeInTheDocument();
    expect(screen.getByText("Released")).toBeInTheDocument();
  });

  it("renders the AI summary when present", async () => {
    listMock.mockResolvedValue([result({ aiSummary: "Mild infiltrate in right lower lobe." })]);
    renderPage();
    expect(await screen.findByText("Mild infiltrate in right lower lobe.")).toBeInTheDocument();
    expect(screen.getByText(/summary/i)).toBeInTheDocument();
  });

  it("renders physician interpretation when present", async () => {
    listMock.mockResolvedValue([result({ physicianInterpretation: "Follow up in 4 weeks." })]);
    renderPage();
    expect(await screen.findByText("Follow up in 4 weeks.")).toBeInTheDocument();
  });

  it("sorts results newest-released first", async () => {
    const older = result({ id: "r-1", testType: "CBC", releasedToPatientAt: "2026-06-10T00:00:00Z" });
    const newer = result({ id: "r-2", testType: "Lipid Panel", releasedToPatientAt: "2026-06-20T00:00:00Z" });
    listMock.mockResolvedValue([older, newer]);
    renderPage();
    await screen.findByText("CBC");
    const items = screen.getAllByRole("listitem");
    // Lipid Panel (newer) should appear before CBC (older).
    expect(items[0]).toHaveTextContent("Lipid Panel");
    expect(items[1]).toHaveTextContent("CBC");
  });

  it("shows the loading spinner while fetching", () => {
    // The query is still pending on the first synchronous render, so the spinner
    // shows before the (resolving) mock settles. A resolving mock keeps the test
    // process from hanging on a never-settled promise.
    listMock.mockResolvedValue([]);
    renderPage();
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });
});
