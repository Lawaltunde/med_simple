import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { SymptomChat } from "./SymptomChat";

vi.mock("@/auth/AuthProvider", () => ({ useAuth: () => ({ user: { id: "p1", role: "PATIENT" } }) }));

const sendSymptomChat = vi.fn();
vi.mock("@/features/chat/api", () => ({ sendSymptomChat: (m: unknown) => sendSymptomChat(m) }));
vi.mock("@/features/symptom/api", () => ({ submitTextSymptomCheck: vi.fn() }));

function renderChat() {
  return render(
    <MemoryRouter>
      <SymptomChat />
    </MemoryRouter>
  );
}

describe("SymptomChat", () => {
  beforeEach(() => sendSymptomChat.mockReset());

  it("sends a message and shows the assistant reply + clinician handoff", async () => {
    sendSymptomChat.mockResolvedValue({
      reply: "It could be a common cold. Please see a clinician if it worsens.",
      urgency: "ROUTINE",
      emergency: false,
      redFlags: [],
      symptomSummary: "Sore throat and runny nose, two days.",
      disclaimer: "This is general health information, not a medical diagnosis.",
    });
    const user = userEvent.setup();
    renderChat();

    await user.type(screen.getByLabelText(/describe your symptoms/i), "sore throat and runny nose");
    await user.click(screen.getByRole("button", { name: /send/i }));

    expect(await screen.findByText(/could be a common cold/i)).toBeInTheDocument();
    // Non-emergency -> clinician handoff offered, no 911 banner.
    expect(screen.getByRole("button", { name: /send these symptoms to a doctor/i })).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shows the 911 emergency banner when the assistant escalates", async () => {
    sendSymptomChat.mockResolvedValue({
      reply: "Call 911 now.",
      urgency: "EMERGENCY",
      emergency: true,
      redFlags: ["chest pain"],
      symptomSummary: "Possible cardiac emergency.",
      disclaimer: "This is general health information, not a medical diagnosis.",
    });
    const user = userEvent.setup();
    renderChat();

    await user.type(screen.getByLabelText(/describe your symptoms/i), "crushing chest pain");
    await user.click(screen.getByRole("button", { name: /send/i }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(/911/);
  });
});
