import { cn } from "@/lib/utils";
import type { EpisodeStatus, UrgencyLevel } from "./types";

// Maps each urgency to its design-system token, label, and what the patient should do.
const URGENCY: Record<UrgencyLevel, { label: string; color: string; advice: string }> = {
  EMERGENCY: {
    label: "Emergency",
    color: "var(--color-urgency-emergency)",
    advice: "Call 911 or go to the nearest emergency department now.",
  },
  URGENT: {
    label: "Urgent",
    color: "var(--color-urgency-urgent)",
    advice: "You should be seen today. A clinician will reach out shortly.",
  },
  SEE_SOON: {
    label: "See soon",
    color: "var(--color-urgency-soon)",
    advice: "This should be looked at in the next day or two.",
  },
  ROUTINE: {
    label: "Routine",
    color: "var(--color-urgency-routine)",
    advice: "Not urgent. A clinician will review and follow up.",
  },
};

export function urgencyMeta(level: UrgencyLevel) {
  return URGENCY[level];
}

export function UrgencyBadge({ level, className }: { level: UrgencyLevel; className?: string }) {
  const meta = URGENCY[level];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold uppercase tracking-wide text-white",
        className
      )}
      style={{ backgroundColor: meta.color }}
    >
      {meta.label}
    </span>
  );
}

// Human-readable episode status for patient-facing screens.
const STATUS_LABEL: Record<EpisodeStatus, string> = {
  SUBMITTED: "Submitted",
  AI_ANALYZED: "Reviewed by AI",
  ASSIGNED: "With a clinician",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
  EMERGENCY_DISPATCHED: "Emergency dispatched",
};

export function statusLabel(status: EpisodeStatus): string {
  return STATUS_LABEL[status];
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}
