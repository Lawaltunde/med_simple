import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/api";
import {
  getCarePlanRequisitions,
  createRequisition,
  sendRequisition,
  cancelRequisition,
  recordResult,
  addInterpretation,
  releaseResult,
  listPatientResults,
} from "./api";

vi.mock("@/lib/api", () => ({ api: vi.fn() }));
const mockApi = vi.mocked(api);
const bodyOf = (i: number) => JSON.parse((mockApi.mock.calls[i][1] as RequestInit).body as string);

describe("diagnostic api", () => {
  beforeEach(() => mockApi.mockReset().mockResolvedValue({}));

  // ── Requisitions ────────────────────────────────────────────────────────────

  it("getCarePlanRequisitions calls the care-plan requisitions path", async () => {
    await getCarePlanRequisitions("cp-1");
    expect(mockApi.mock.calls[0][0]).toBe("/care-plans/cp-1/requisitions");
  });

  it("createRequisition POSTs all required fields", async () => {
    await createRequisition({ carePlanId: "cp-1", testType: "CBC", urgency: "ROUTINE" });
    expect(mockApi.mock.calls[0][0]).toBe("/requisitions");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("POST");
    const body = bodyOf(0);
    expect(body.carePlanId).toBe("cp-1");
    expect(body.testType).toBe("CBC");
    expect(body.urgency).toBe("ROUTINE");
  });

  it("createRequisition includes optional fields when provided", async () => {
    await createRequisition({
      carePlanId: "cp-1",
      testType: "CBC",
      urgency: "URGENT",
      clinicalIndication: "Anaemia screen",
      testCodes: ["58410-2"],
    });
    const body = bodyOf(0);
    expect(body.clinicalIndication).toBe("Anaemia screen");
    expect(body.testCodes).toEqual(["58410-2"]);
  });

  it("sendRequisition POSTs to the send path with no body", async () => {
    await sendRequisition("req-1");
    expect(mockApi.mock.calls[0][0]).toBe("/requisitions/req-1/send");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("POST");
  });

  it("cancelRequisition POSTs to the cancel path", async () => {
    await cancelRequisition("req-1");
    expect(mockApi.mock.calls[0][0]).toBe("/requisitions/req-1/cancel");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("POST");
  });

  // ── Results: physician side ─────────────────────────────────────────────────

  it("recordResult POSTs raw result text to the result path", async () => {
    await recordResult("req-1", { rawResultText: "WBC 7.2, HGB 138" });
    expect(mockApi.mock.calls[0][0]).toBe("/requisitions/req-1/result");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("POST");
    expect(bodyOf(0).rawResultText).toBe("WBC 7.2, HGB 138");
  });

  it("addInterpretation POSTs to the interpretation path", async () => {
    await addInterpretation("res-1", { physicianInterpretation: "All normal." });
    expect(mockApi.mock.calls[0][0]).toBe("/results/res-1/interpretation");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("POST");
    expect(bodyOf(0).physicianInterpretation).toBe("All normal.");
  });

  it("releaseResult POSTs to the release path with no body", async () => {
    await releaseResult("res-1");
    expect(mockApi.mock.calls[0][0]).toBe("/results/res-1/release");
    expect((mockApi.mock.calls[0][1] as RequestInit).method).toBe("POST");
  });

  // ── Results: patient side ───────────────────────────────────────────────────

  it("listPatientResults calls the patient results endpoint (release-gated)", async () => {
    await listPatientResults("patient-1");
    // Asserts the PATIENT endpoint is used, not the physician endpoint.
    // The backend gate (released_to_patient_at non-NULL) is enforced server-side
    // inside DiagnosticService.getPatientReleasedResults().
    expect(mockApi.mock.calls[0][0]).toBe("/patients/patient-1/results");
    // Not the physician endpoint /results/:id
    expect(mockApi.mock.calls[0][0]).not.toContain("/results/patient-1");
  });
});
