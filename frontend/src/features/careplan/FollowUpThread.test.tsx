import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { FollowUpThread } from "./FollowUpThread";
import type { CarePlan } from "./types";

const addMutate = vi.fn();
const respondMutate = vi.fn();
vi.mock("./hooks", () => ({
  useAddFollowUp: () => ({ mutate: addMutate, isPending: false }),
  useRespondToFollowUp: () => ({ mutate: respondMutate, isPending: false }),
}));

// A plan with one physician message that hasn't been answered.
function plan(): CarePlan {
  return {
    id: "cp1",
    episodeId: "ep1",
    patientId: "pat1",
    physicianId: "doc1",
    clinicId: "c1",
    status: "ACTIVE",
    notes: "Rest and fluids",
    createdAt: "2026-06-17T10:00:00Z",
    updatedAt: "2026-06-17T10:00:00Z",
    followUps: [
      {
        id: "f1", initiatedBy: "doc1", system: false, message: "How are you feeling after 3 days?",
        response: null, respondedBy: null, respondedAt: null, createdAt: "2026-06-17T11:00:00Z",
      },
    ],
  };
}

describe("FollowUpThread", () => {
  beforeEach(() => {
    addMutate.mockReset();
    respondMutate.mockReset();
  });

  it("lets the patient reply to an unanswered physician message", async () => {
    const user = userEvent.setup();
    render(<FollowUpThread plan={plan()} currentUserId="pat1" />);

    expect(screen.getByText("How are you feeling after 3 days?")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Reply"), "Much better, thanks");
    await user.click(screen.getByRole("button", { name: "Send reply" }));

    expect(respondMutate).toHaveBeenCalledWith(
      { followUpId: "f1", respondedBy: "pat1", response: "Much better, thanks" },
      expect.anything()
    );
  });

  it("does not let the author reply to their own message", () => {
    render(<FollowUpThread plan={plan()} currentUserId="doc1" />);
    // doc1 wrote the message, so no reply box is offered for it.
    expect(screen.queryByLabelText("Reply")).not.toBeInTheDocument();
  });

  it("sends a new message tagged with the current user as initiator", async () => {
    const user = userEvent.setup();
    render(<FollowUpThread plan={plan()} currentUserId="pat1" />);

    await user.type(screen.getByLabelText("New message"), "One more question");
    await user.click(screen.getByRole("button", { name: "Send message" }));

    expect(addMutate).toHaveBeenCalledWith(
      { initiatedBy: "pat1", message: "One more question" },
      expect.anything()
    );
  });
});
