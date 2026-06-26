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
  usePatientAppointments: () => ({ data: [] }),
}));

const episode = { id: "ep-1", patientId: "pat-1" } as Episode;

function renderCard() {
  return render(<BookAppointmentCard episode={episode} episodeId="ep-1" />);
}

async function clickDay(user: ReturnType<typeof userEvent.setup>, target: Date) {
  let btn = screen.queryByLabelText(target.toDateString());
  if (!btn) {
    await user.click(screen.getByLabelText("Next month"));
    btn = screen.getByLabelText(target.toDateString());
  }
  await user.click(btn);
}

describe("BookAppointmentCard — AI window highlight", () => {
  beforeEach(() => {
    suggestionData.mockReset();
    createMutate.mockReset();
  });

  it("does not show the AI recommendation when there is no suggested follow-up", () => {
    suggestionData.mockReturnValue(undefined);
    renderCard();
    expect(screen.queryByText(/AI recommends a follow-up/i)).not.toBeInTheDocument();
  });

  it("shows the AI window as a recommendation, never a committed date", () => {
    suggestionData.mockReturnValue({ status: "PROPOSED", suggestedFollowUpDays: 3 });
    renderCard();
    // The AI recommends a window and tells the physician to pick the day — it does not pre-select one.
    expect(screen.getByText(/AI recommends a follow-up around/i)).toBeInTheDocument();
    expect(screen.getByText(/Pick a day on the calendar/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /book appointment/i })).toBeDisabled();
  });

  it("books the day the physician picks on the calendar", async () => {
    suggestionData.mockReturnValue({ status: "PROPOSED", suggestedFollowUpDays: 3 });
    const user = userEvent.setup();
    renderCard();

    const target = new Date();
    target.setDate(target.getDate() + 4);
    await clickDay(user, target);

    await user.click(screen.getByRole("button", { name: /book appointment/i }));
    await waitFor(() => expect(createMutate).toHaveBeenCalled());

    const [payload] = createMutate.mock.calls[0];
    expect(payload).toMatchObject({ episodeId: "ep-1", physicianId: "doc-1" });
    // The booked date matches the day the physician clicked (not an AI-chosen date).
    expect(new Date(payload.scheduledAt).getDate()).toBe(target.getDate());
  });
});
