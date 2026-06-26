import { describe, it, expect } from "vitest";
import { homePathForRole } from "./types";

describe("homePathForRole", () => {
  it("sends patients to the patient app", () => {
    expect(homePathForRole("PATIENT")).toBe("/app");
  });

  it("sends clinical staff to the clinician console", () => {
    expect(homePathForRole("PHYSICIAN")).toBe("/console");
    expect(homePathForRole("NURSE")).toBe("/console");
    expect(homePathForRole("RECEPTIONIST")).toBe("/console");
  });

  it("sends administrators to the admin console, not the clinician console", () => {
    expect(homePathForRole("CLINIC_ADMIN")).toBe("/admin");
    expect(homePathForRole("SUPER_ADMIN")).toBe("/admin");
  });
});
