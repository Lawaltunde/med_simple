import { describe, it, expect } from "vitest";
import { queueStats } from "./display";
import type { QueueEntry } from "./types";
import type { UrgencyLevel } from "@/features/symptom/types";

function entry(urgency: UrgencyLevel | null): QueueEntry {
  return {
    episodeId: "e",
    patientId: "p",
    patientFirstName: "A",
    patientLastName: "B",
    effectiveUrgency: urgency,
    aiUrgencyLevel: urgency,
    confirmedUrgencyLevel: null,
    status: "AI_ANALYZED",
    aiSummary: null,
    assignedPhysicianId: null,
    createdAt: "2026-01-01T00:00:00Z",
  };
}

describe("queueStats", () => {
  it("returns null when the queue has not loaded yet", () => {
    expect(queueStats(undefined)).toBeNull();
  });

  it("returns null when the queue is empty (the empty-state card speaks instead)", () => {
    expect(queueStats([])).toBeNull();
  });

  it("counts the total when nothing is hot", () => {
    expect(queueStats([entry("ROUTINE"), entry("SEE_SOON"), entry(null)])).toEqual({
      total: 3,
      emergency: 0,
      urgent: 0,
    });
  });

  it("breaks out emergency and urgent counts", () => {
    const q = [
      entry("EMERGENCY"),
      entry("URGENT"),
      entry("URGENT"),
      entry("ROUTINE"),
      entry("SEE_SOON"),
      entry(null),
      entry("SEE_SOON"),
    ];
    expect(queueStats(q)).toEqual({ total: 7, emergency: 1, urgent: 2 });
  });
});
