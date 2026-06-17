import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import { submitTextSymptomCheck } from "./api";
import type { Episode } from "./types";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);

function episode(overrides: Partial<Episode> = {}): Episode {
  return {
    id: "ep1",
    patientId: "p1",
    assignedClinicId: null,
    assignedPhysicianId: null,
    status: "SUBMITTED",
    aiUrgencyLevel: null,
    aiSummary: null,
    aiAnalyzedAt: null,
    confirmedUrgencyLevel: null,
    createdAt: "2026-06-17T12:00:00Z",
    updatedAt: "2026-06-17T12:00:00Z",
    submissions: [],
    ...overrides,
  };
}

describe("submitTextSymptomCheck", () => {
  beforeEach(() => mockApi.mockReset());

  it("creates the episode, adds the text submission, then triages — in that order", async () => {
    mockApi
      .mockResolvedValueOnce(episode({ id: "ep1", status: "SUBMITTED" })) // start
      .mockResolvedValueOnce({ id: "sub1" }) // addSubmission
      .mockResolvedValueOnce(episode({ id: "ep1", status: "AI_ANALYZED", aiUrgencyLevel: "URGENT" })); // triage

    const result = await submitTextSymptomCheck({ patientId: "p1", description: "sore throat for two days" });

    expect(mockApi).toHaveBeenCalledTimes(3);

    // 1. start episode
    expect(mockApi.mock.calls[0][0]).toBe("/patients/p1/episodes");
    expect(mockApi.mock.calls[0][1]).toMatchObject({ method: "POST" });
    expect(JSON.parse((mockApi.mock.calls[0][1] as RequestInit).body as string)).toEqual({ clinicId: null });

    // 2. add submission, keyed to the new episode id
    expect(mockApi.mock.calls[1][0]).toBe("/episodes/ep1/submissions");
    expect(JSON.parse((mockApi.mock.calls[1][1] as RequestInit).body as string)).toEqual({
      inputType: "TEXT",
      textContent: "sore throat for two days",
    });

    // 3. triage the same episode
    expect(mockApi.mock.calls[2][0]).toBe("/episodes/ep1/triage");

    // Resolves to the triaged episode.
    expect(result.status).toBe("AI_ANALYZED");
    expect(result.aiUrgencyLevel).toBe("URGENT");
  });

  it("passes a chosen clinicId through to episode creation", async () => {
    mockApi
      .mockResolvedValueOnce(episode())
      .mockResolvedValueOnce({ id: "sub1" })
      .mockResolvedValueOnce(episode({ status: "AI_ANALYZED" }));

    await submitTextSymptomCheck({ patientId: "p1", description: "x".repeat(10), clinicId: "clinic-9" });

    expect(JSON.parse((mockApi.mock.calls[0][1] as RequestInit).body as string)).toEqual({ clinicId: "clinic-9" });
  });

  it("stops if episode creation fails and never submits or triages", async () => {
    mockApi.mockRejectedValueOnce(new Error("boom"));

    await expect(submitTextSymptomCheck({ patientId: "p1", description: "anything here" })).rejects.toThrow("boom");
    expect(mockApi).toHaveBeenCalledTimes(1);
  });
});
