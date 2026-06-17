import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NewSymptomCheck } from "./NewSymptomCheck";

// Authenticated patient.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

// Stub the network: the composed flow resolves to a triaged episode.
const submitMock = vi.fn();
vi.mock("@/features/symptom/api", () => ({
  submitTextSymptomCheck: (...args: unknown[]) => submitMock(...args),
}));

// Capture navigation.
const navigateMock = vi.fn();
vi.mock("react-router-dom", async (orig) => ({
  ...(await orig<typeof import("react-router-dom")>()),
  useNavigate: () => navigateMock,
}));

function renderScreen() {
  const qc = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <NewSymptomCheck />
    </QueryClientProvider>
  );
}

describe("NewSymptomCheck", () => {
  beforeEach(() => {
    submitMock.mockReset();
    navigateMock.mockReset();
  });

  it("blocks submission when the description is too short", async () => {
    const user = userEvent.setup();
    renderScreen();

    await user.type(screen.getByLabelText("What's going on?"), "sore");
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    expect(await screen.findByText(/at least 10 characters/i)).toBeInTheDocument();
    expect(submitMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it("submits a valid description and navigates to the triaged episode", async () => {
    submitMock.mockResolvedValueOnce({ id: "ep-42", patientId: "patient-1" });
    const user = userEvent.setup();
    renderScreen();

    await user.type(screen.getByLabelText("What's going on?"), "sore throat and fever for two days");
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    await waitFor(() =>
      expect(submitMock).toHaveBeenCalledWith({
        patientId: "patient-1",
        description: "sore throat and fever for two days",
      })
    );
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith("/app/episodes/ep-42"));
  });

  it("surfaces an error message when triage fails", async () => {
    submitMock.mockRejectedValueOnce(new Error("network down"));
    const user = userEvent.setup();
    renderScreen();

    await user.type(screen.getByLabelText("What's going on?"), "persistent cough for a week");
    await user.click(screen.getByRole("button", { name: /submit for triage/i }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
