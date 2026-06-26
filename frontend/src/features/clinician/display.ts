import type { QueueEntry } from "./types";

export interface QueueStats {
  total: number;
  emergency: number;
  urgent: number;
}

/**
 * Operational counts for the triage-queue header: how many complaints are waiting, and how many
 * are hot. Returns null when the queue is empty or not loaded yet, so the header can render just
 * its title and clinic name (the empty-state card already says the queue is clear).
 *
 * SEE_SOON/ROUTINE fold into the total rather than cluttering the header — only the levels a
 * physician acts on right now (emergency, urgent) are broken out.
 */
export function queueStats(queue: QueueEntry[] | undefined): QueueStats | null {
  if (!queue || queue.length === 0) return null;
  return {
    total: queue.length,
    emergency: queue.filter((e) => e.effectiveUrgency === "EMERGENCY").length,
    urgent: queue.filter((e) => e.effectiveUrgency === "URGENT").length,
  };
}
