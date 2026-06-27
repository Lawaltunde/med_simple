import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import { uploadMedia } from "@/features/media/api";
import { submitTextSymptomCheck, submitMediaSymptomCheck } from "./api";
import type { Episode } from "./types";

vi.mock("@/lib/api", () => ({ api: vi.fn(), tokenStore: { get: () => "tok" } }));
vi.mock("@/features/media/api", async (orig) => ({
  ...(await orig<typeof import("@/features/media/api")>()),
  uploadMedia: vi.fn(),
}));
const mockApi = vi.mocked(api);
const mockUpload = vi.mocked(uploadMedia);

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

describe("submitMediaSymptomCheck", () => {
  beforeEach(() => {
    mockApi.mockReset();
    mockUpload.mockReset();
  });

  it("uploads, creates the episode, adds the media submission, processes it, then triages — in order", async () => {
    mockUpload.mockResolvedValueOnce({ mediaUrl: "/api/v1/media/m1", contentType: "image/png" });
    mockApi
      .mockResolvedValueOnce(episode({ id: "ep5", status: "SUBMITTED" })) // start
      .mockResolvedValueOnce({ id: "sub9" }) // addSubmission
      .mockResolvedValueOnce({ id: "sub9", processingStatus: "COMPLETED" }) // process
      .mockResolvedValueOnce(episode({ id: "ep5", status: "AI_ANALYZED", aiUrgencyLevel: "SEE_SOON" })); // triage

    const blob = new Blob([new Uint8Array([1])], { type: "image/png" });
    const result = await submitMediaSymptomCheck({ patientId: "p1", inputType: "PHOTO", blob });

    // upload first, then 4 api calls: start, add, process, triage.
    expect(mockUpload).toHaveBeenCalledTimes(1);
    expect(mockApi).toHaveBeenCalledTimes(4);

    // 1. start episode
    expect(mockApi.mock.calls[0][0]).toBe("/patients/p1/episodes");
    // 2. media submission carries the uploaded mediaUrl
    expect(mockApi.mock.calls[1][0]).toBe("/episodes/ep5/submissions");
    expect(JSON.parse((mockApi.mock.calls[1][1] as RequestInit).body as string)).toEqual({
      inputType: "PHOTO",
      mediaUrl: "/api/v1/media/m1",
    });
    // 3. process the submission synchronously
    expect(mockApi.mock.calls[2][0]).toBe("/submissions/sub9/process");
    expect((mockApi.mock.calls[2][1] as RequestInit).method).toBe("POST");
    // 4. triage the episode
    expect(mockApi.mock.calls[3][0]).toBe("/episodes/ep5/triage");

    expect(result.kind).toBe("triaged");
    if (result.kind === "triaged") expect(result.episode.status).toBe("AI_ANALYZED");
  });

  it("does NOT triage a photo the AI saw no symptom in — returns needs_review instead", async () => {
    mockUpload.mockResolvedValueOnce({ mediaUrl: "/api/v1/media/m2", contentType: "image/png" });
    mockApi
      .mockResolvedValueOnce(episode({ id: "ep6", status: "SUBMITTED" })) // start
      .mockResolvedValueOnce({ id: "sub10" }) // addSubmission
      .mockResolvedValueOnce({
        id: "sub10",
        processingStatus: "COMPLETED",
        aiAnalysisRelevance: "NO_SYMPTOM",
        aiAnalysisResult: "This looks like a photo of a dog, not a symptom.",
      }); // process

    const blob = new Blob([new Uint8Array([1])], { type: "image/png" });
    const result = await submitMediaSymptomCheck({ patientId: "p1", inputType: "PHOTO", blob });

    // Stops after process — no triage call.
    expect(mockApi).toHaveBeenCalledTimes(3);
    expect(mockApi.mock.calls.some((c) => String(c[0]).endsWith("/triage"))).toBe(false);

    expect(result.kind).toBe("needs_review");
    if (result.kind === "needs_review") {
      expect(result.episodeId).toBe("ep6");
      expect(result.description).toBe("This looks like a photo of a dog, not a symptom.");
    }
  });
});
