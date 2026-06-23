import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import { listPatientAppointments, listClinicAppointments, createAppointment } from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn(), tokenStore: { get: () => "tok" } }));
const mockApi = vi.mocked(api);
const bodyOf = (i: number) => JSON.parse((mockApi.mock.calls[i][1] as RequestInit).body as string);
const methodOf = (i: number) => (mockApi.mock.calls[i][1] as RequestInit | undefined)?.method;

describe("appointment api", () => {
  beforeEach(() => mockApi.mockReset().mockResolvedValue({}));

  it("listPatientAppointments GETs the patient path", async () => {
    await listPatientAppointments("pat-1");
    expect(mockApi.mock.calls[0][0]).toBe("/patients/pat-1/appointments");
    expect(methodOf(0)).toBeUndefined();
  });

  it("listClinicAppointments GETs the clinic path", async () => {
    await listClinicAppointments("clinic-1");
    expect(mockApi.mock.calls[0][0]).toBe("/clinics/clinic-1/appointments");
  });

  it("createAppointment POSTs episode, physician, time, duration and reason", async () => {
    await createAppointment({
      episodeId: "ep-1", physicianId: "doc-1",
      scheduledAt: "2026-07-01T14:30:00.000Z", durationMinutes: 20, reason: "Follow-up",
    });
    expect(mockApi.mock.calls[0][0]).toBe("/appointments");
    expect(methodOf(0)).toBe("POST");
    expect(bodyOf(0)).toEqual({
      episodeId: "ep-1", physicianId: "doc-1",
      scheduledAt: "2026-07-01T14:30:00.000Z", durationMinutes: 20, reason: "Follow-up",
    });
  });
});
