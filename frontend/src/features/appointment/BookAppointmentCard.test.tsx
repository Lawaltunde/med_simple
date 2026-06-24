import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BookAppointmentCard } from "./BookAppointmentCard";
import type { Episode } from "@/features/symptom/types";

vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "doc-1", firstName: "Dana", lastName: "Okafor", role: "PHYSICIAN" } }),
}));

const suggestionData = vi.fn();
vi.mock("@/features/careplan/hooks", () => ({
  useEpisodeSuggestion: () => ({ data: suggestionData() }),
}));

const createMutate = vi.fn();
vi.mock("./hooks", () => ({
  useCreateAppointment: () => ({ mutate: createMutate, isPending: false }),
}));

const episode = { id: "ep-1", patientId: "pat-1" } as Episode;

function renderCard() {
  return render(<BookAppointmentCard episode={episode} episodeId="ep-1" />);
}

describe("BookAppointmentCard — AI follow-up", () => {
  beforeEach(() => {
    suggestionData.mockReset();
    createMutate.mockReset();
  });

  it("does not show the AI banner when there is no suggested follow-up", () => {
    suggestionData.mockReturnValue(undefined);
    renderCard();
    expect(screen.queryByText(/AI suggests a follow-up/i)).not.toBeInTheDocument();
  });

  it("shows the AI follow-up banner and pre-fills the form, then books it", async () => {
    suggestionData.mockReturnValue({ status: "PROPOSED", suggestedFollowUpDays: 3 });
    const user = userEvent.setup();
    const { container } = renderCard();

    expect(screen.getByText(/AI suggests a follow-up in 3 days/i)).toBeInTheDocument();

    const dateInput = container.querySelector('input[type="datetime-local"]') as HTMLInputElement;
    expect(dateInput.value).toBe(""); // not pre-filled until the physician opts in

    await user.click(screen.getByRole("button", { name: /use this/i }));
    expect(dateInput.value).not.toBe(""); // now pre-filled to ~3 days out

    await user.click(screen.getByRole("button", { name: /book appointment/i }));
    await waitFor(() => expect(createMutate).toHaveBeenCalled());
    const [payload] = createMutate.mock.calls[0];
    expect(payload).toMatchObject({ episodeId: "ep-1", physicianId: "doc-1", reason: "Follow-up" });
    expect(payload.scheduledAt).toBeTruthy();
  });
});
