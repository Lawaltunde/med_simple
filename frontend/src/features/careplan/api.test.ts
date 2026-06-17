import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import { addFollowUp, createCarePlan, respondToFollowUp, updateCarePlan } from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);
const bodyOf = (i: number) => JSON.parse((mockApi.mock.calls[i][1] as RequestInit).body as string);

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
});
