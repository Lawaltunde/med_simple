import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import { assignPhysician, confirmUrgency } from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);

function bodyOf(callIndex: number) {
  return JSON.parse((mockApi.mock.calls[callIndex][1] as RequestInit).body as string);
}

describe("clinician api", () => {
  beforeEach(() => mockApi.mockReset().mockResolvedValue({}));

  it("assignPhysician posts the physician id to the episode", async () => {
    await assignPhysician("ep1", "doc1");
    expect(mockApi.mock.calls[0][0]).toBe("/episodes/ep1/assign");
    expect(bodyOf(0)).toEqual({ physicianId: "doc1" });
  });

  it("confirmUrgency without an override sends a null reason", async () => {
    await confirmUrgency({ episodeId: "ep1", physicianId: "doc1", confirmedUrgencyLevel: "ROUTINE" });
    expect(mockApi.mock.calls[0][0]).toBe("/episodes/ep1/confirm-urgency");
    expect(bodyOf(0)).toEqual({ physicianId: "doc1", confirmedUrgencyLevel: "ROUTINE", overrideReason: null });
  });

  it("confirmUrgency carries the override reason when overriding", async () => {
    await confirmUrgency({
      episodeId: "ep1",
      physicianId: "doc1",
      confirmedUrgencyLevel: "URGENT",
      overrideReason: "Red flags the AI missed",
    });
    expect(bodyOf(0)).toEqual({
      physicianId: "doc1",
      confirmedUrgencyLevel: "URGENT",
      overrideReason: "Red flags the AI missed",
    });
  });
});
