import type { RequisitionStatus, RequisitionUrgency } from "./types";

const STATUS_META: Record<RequisitionStatus, { label: string; className: string }> = {
  DRAFT: { label: "Draft", className: "bg-canvas text-muted" },
  SENT: { label: "Sent", className: "bg-brand-50 text-brand-700" },
  BOOKED: { label: "Booked", className: "bg-brand-100 text-brand-700" },
  IN_PROGRESS: { label: "In progress", className: "bg-yellow-50 text-yellow-700" },
  RESULTED: { label: "Resulted", className: "bg-green-50 text-green-700" },
  CANCELLED: { label: "Cancelled", className: "bg-canvas text-muted line-through" },
};

const URGENCY_META: Record<RequisitionUrgency, { label: string; className: string }> = {
  ROUTINE: { label: "Routine", className: "bg-canvas text-muted" },
  URGENT: { label: "Urgent", className: "bg-yellow-50 text-yellow-700" },
  STAT: { label: "STAT", className: "bg-red-50 text-red-700" },
};

export function RequisitionStatusBadge({ status }: { status: RequisitionStatus }) {
  const meta = STATUS_META[status];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide ${meta.className}`}
    >
      {meta.label}
    </span>
  );
}

export function UrgencyBadge({ urgency }: { urgency: RequisitionUrgency }) {
  const meta = URGENCY_META[urgency];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide ${meta.className}`}
    >
      {meta.label}
    </span>
  );
}
