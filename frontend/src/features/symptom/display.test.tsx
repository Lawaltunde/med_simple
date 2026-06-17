import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { UrgencyBadge, urgencyMeta, statusLabel } from "./display";
import { effectiveUrgency, type Episode } from "./types";

describe("urgency display", () => {
  it("maps every urgency level to a label, color, and advice", () => {
    for (const level of ["ROUTINE", "SEE_SOON", "URGENT", "EMERGENCY"] as const) {
      const meta = urgencyMeta(level);
      expect(meta.label).toBeTruthy();
      expect(meta.color).toMatch(/^var\(--color-urgency-/);
      expect(meta.advice).toBeTruthy();
    }
  });

  it("emergency advice tells the patient to call 911", () => {
    expect(urgencyMeta("EMERGENCY").advice.toLowerCase()).toContain("911");
  });

  it("renders the badge with the level's label", () => {
    render(<UrgencyBadge level="URGENT" />);
    expect(screen.getByText("Urgent")).toBeInTheDocument();
  });
});

describe("statusLabel", () => {
  it("renders a human label for AI_ANALYZED", () => {
    expect(statusLabel("AI_ANALYZED")).toBe("Reviewed by AI");
  });
});

describe("effectiveUrgency", () => {
  const base: Episode = {
    id: "e",
    patientId: "p",
    assignedClinicId: null,
    assignedPhysicianId: null,
    status: "AI_ANALYZED",
    aiUrgencyLevel: "ROUTINE",
    aiSummary: null,
    aiAnalyzedAt: null,
    confirmedUrgencyLevel: null,
    createdAt: "2026-06-17T12:00:00Z",
    updatedAt: "2026-06-17T12:00:00Z",
    submissions: [],
  };

  it("uses the AI level when no physician has confirmed", () => {
    expect(effectiveUrgency(base)).toBe("ROUTINE");
  });

  it("prefers the physician's confirmed level over the AI guess", () => {
    expect(effectiveUrgency({ ...base, confirmedUrgencyLevel: "URGENT" })).toBe("URGENT");
  });

  it("returns null before any triage has run", () => {
    expect(effectiveUrgency({ ...base, aiUrgencyLevel: null })).toBeNull();
  });
});
