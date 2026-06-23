import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import {
  addFollowUp,
  approveSuggestion,
  createCarePlan,
  getEpisodeSuggestion,
  rejectSuggestion,
  respondToFollowUp,
  updateCarePlan,
} from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);
const bodyOf = (i: number) => JSON.parse((mockApi.mock.calls[i][1] as RequestInit).body as string);
const methodOf = (i: number) => (mockApi.mock.calls[i][1] as RequestInit | undefined)?.method;

describe("care plan api", () => {
  beforeEach(() => mockApi.mockReset().mockResolvedValue({}));

  it("createCarePlan posts episode, physician, and notes", async () => {
    await createCarePlan({ episodeId: "ep1", physicianId: "doc1", notes: "Rest and fluids" });
    expect(mockApi.mock.calls[0][0]).toBe("/care-plans");
    expect(bodyOf(0)).toEqual({ episodeId: "ep1", physicianId: "doc1", notes: "Rest and fluids" });
  });

  it("updateCarePlan PATCHes status to complete", async () => {
    await updateCarePlan("cp1", { status: "COMPLETED" });
    expect(mockApi.mock.calls[0][0]).toBe("/care-plans/cp1");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("PATCH");
    expect(bodyOf(0)).toEqual({ status: "COMPLETED" });
  });

  it("addFollowUp always marks the message as non-system", async () => {
    await addFollowUp("cp1", { initiatedBy: "doc1", message: "How are you?" });
    expect(mockApi.mock.calls[0][0]).toBe("/care-plans/cp1/follow-ups");
    expect(bodyOf(0)).toEqual({ initiatedBy: "doc1", message: "How are you?", system: false });
  });

  it("respondToFollowUp posts to the nested respond path", async () => {
    await respondToFollowUp("cp1", "f9", { respondedBy: "pat1", response: "Much better" });
    expect(mockApi.mock.calls[0][0]).toBe("/care-plans/cp1/follow-ups/f9/respond");
    expect(bodyOf(0)).toEqual({ respondedBy: "pat1", response: "Much better" });
  });

  it("getEpisodeSuggestion GETs the staff-only episode suggestion path", async () => {
    await getEpisodeSuggestion("ep1");
    expect(mockApi.mock.calls[0][0]).toBe("/episodes/ep1/care-suggestion");
    expect(methodOf(0)).toBeUndefined(); // default GET
  });

  it("approveSuggestion posts physician and edited notes to the approve path", async () => {
    await approveSuggestion("sg1", { physicianId: "doc1", editedNotes: "Revised plan" });
    expect(mockApi.mock.calls[0][0]).toBe("/care-suggestions/sg1/approve");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toEqual({ physicianId: "doc1", editedNotes: "Revised plan" });
  });

  it("rejectSuggestion posts to the reject path", async () => {
    await rejectSuggestion("sg1", { physicianId: "doc1", reason: "Disagree" });
    expect(mockApi.mock.calls[0][0]).toBe("/care-suggestions/sg1/reject");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toEqual({ physicianId: "doc1", reason: "Disagree" });
  });
});
