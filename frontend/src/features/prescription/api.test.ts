import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import {
  cancelPrescription,
  createPrescription,
  getPrescription,
  listEpisodePrescriptions,
  listPatientPrescriptions,
  sendPrescription,
  signPrescription,
} from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);
const bodyOf = (i: number) => JSON.parse((mockApi.mock.calls[i][1] as RequestInit).body as string);
const methodOf = (i: number) => (mockApi.mock.calls[i][1] as RequestInit).method;

describe("prescription api", () => {
  beforeEach(() => mockApi.mockReset().mockResolvedValue({}));

  it("createPrescription POSTs to /prescriptions with full body", async () => {
    await createPrescription({
      episodeId: "ep-1",
      medicationName: "Amoxicillin",
      dosage: "500 mg",
      frequency: "Three times daily",
      refills: 1,
    });
    expect(mockApi.mock.calls[0][0]).toBe("/prescriptions");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toMatchObject({
      episodeId: "ep-1",
      medicationName: "Amoxicillin",
      dosage: "500 mg",
      frequency: "Three times daily",
      refills: 1,
    });
  });

  it("getPrescription GETs the correct path", async () => {
    await getPrescription("rx-99");
    expect(mockApi.mock.calls[0][0]).toBe("/prescriptions/rx-99");
  });

  it("listEpisodePrescriptions GETs the episode-scoped path", async () => {
    await listEpisodePrescriptions("ep-2");
    expect(mockApi.mock.calls[0][0]).toBe("/episodes/ep-2/prescriptions");
  });

  it("listPatientPrescriptions GETs the patient-scoped path", async () => {
    await listPatientPrescriptions("pt-3");
    expect(mockApi.mock.calls[0][0]).toBe("/patients/pt-3/prescriptions");
  });

  it("signPrescription POSTs to /prescriptions/:id/sign", async () => {
    await signPrescription("rx-1");
    expect(mockApi.mock.calls[0][0]).toBe("/prescriptions/rx-1/sign");
    expect(methodOf(0)).toBe("POST");
  });

  it("sendPrescription POSTs to /prescriptions/:id/send with empty body when no pharmacyId", async () => {
    await sendPrescription("rx-2");
    expect(mockApi.mock.calls[0][0]).toBe("/prescriptions/rx-2/send");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toEqual({});
  });

  it("sendPrescription includes pharmacyId when provided", async () => {
    await sendPrescription("rx-2", { pharmacyId: "pharm-99" });
    expect(bodyOf(0)).toEqual({ pharmacyId: "pharm-99" });
  });

  it("cancelPrescription POSTs to /prescriptions/:id/cancel", async () => {
    await cancelPrescription("rx-3");
    expect(mockApi.mock.calls[0][0]).toBe("/prescriptions/rx-3/cancel");
    expect(methodOf(0)).toBe("POST");
  });
});
