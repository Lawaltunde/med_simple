import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { CAPABILITY_META, VerificationBadge } from "./display";
import type { CapabilityType } from "./api";

describe("clinic admin display", () => {
  it("covers every capability with a label, description, and fallback", () => {
    const all: CapabilityType[] = [
      "PRESCRIBIT", "OCEAN_EREFERRAL", "EMR_FHIR", "LAB_ELECTRONIC", "LAB_BOOKING", "PHARMACY_ORDERING",
    ];
    for (const c of all) {
      expect(CAPABILITY_META[c].label).toBeTruthy();
      expect(CAPABILITY_META[c].description).toBeTruthy();
      expect(CAPABILITY_META[c].fallback).toBeTruthy();
    }
  });

  it("renders the verification status", () => {
    render(<VerificationBadge status="VERIFIED" />);
    expect(screen.getByText(/verified/i)).toBeInTheDocument();
  });
});
