import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import {
  cancelReferral,
  confirmSpecialty,
  createReferral,
  getEpisodeReferrals,
  getPatientReferrals,
  getReferral,
  submitReferral,
} from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);
const bodyOf = (i: number) => JSON.parse((mockApi.mock.calls[i][1] as RequestInit).body as string);
const methodOf = (i: number) => (mockApi.mock.calls[i][1] as RequestInit | undefined)?.method;

describe("referral api", () => {
  beforeEach(() => mockApi.mockReset().mockResolvedValue({}));

  it("createReferral POSTs episodeId, clinicalSummary, and urgency", async () => {
    await createReferral({ episodeId: "ep-1", clinicalSummary: "Chest pain", urgency: "URGENT" });
    expect(mockApi.mock.calls[0][0]).toBe("/referrals");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toEqual({ episodeId: "ep-1", clinicalSummary: "Chest pain", urgency: "URGENT" });
  });

  it("getReferral GETs by id", async () => {
    await getReferral("ref-1");
    expect(mockApi.mock.calls[0][0]).toBe("/referrals/ref-1");
    expect(methodOf(0)).toBeUndefined(); // default GET
  });

  it("getEpisodeReferrals GETs the nested episode path", async () => {
    await getEpisodeReferrals("ep-2");
    expect(mockApi.mock.calls[0][0]).toBe("/episodes/ep-2/referrals");
  });

  it("getPatientReferrals GETs the patient-scoped path", async () => {
    await getPatientReferrals("pat-1");
    expect(mockApi.mock.calls[0][0]).toBe("/patients/pat-1/referrals");
  });

  it("confirmSpecialty POSTs confirmedSpecialty to the confirm-specialty sub-route", async () => {
    await confirmSpecialty("ref-2", "Cardiology");
    expect(mockApi.mock.calls[0][0]).toBe("/referrals/ref-2/confirm-specialty");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toEqual({ confirmedSpecialty: "Cardiology" });
  });

  it("submitReferral POSTs to the submit sub-route with no body", async () => {
    await submitReferral("ref-3");
    expect(mockApi.mock.calls[0][0]).toBe("/referrals/ref-3/submit");
    expect(methodOf(0)).toBe("POST");
    expect(mockApi.mock.calls[0][1]).not.toHaveProperty("body");
  });

  it("cancelReferral POSTs to the cancel sub-route", async () => {
    await cancelReferral("ref-4");
    expect(mockApi.mock.calls[0][0]).toBe("/referrals/ref-4/cancel");
    expect(methodOf(0)).toBe("POST");
  });
});
