import type { ReferralDeliveryMethod, ReferralStatus, ReferralUrgency } from "./types";

// ── Status ─────────────────────────────────────────────────────────────────────

const STATUS_META: Record<ReferralStatus, { label: string; className: string }> = {
  DRAFT: { label: "Draft", className: "bg-canvas text-muted" },
  SUBMITTED: { label: "Submitted", className: "bg-blue-50 text-blue-700" },
  ACCEPTED: { label: "Accepted", className: "bg-brand-100 text-brand-700" },
  COMPLETED: { label: "Completed", className: "bg-brand-50 text-brand-600" },
  CANCELLED: { label: "Cancelled", className: "bg-canvas text-muted line-through" },
};

export function ReferralStatusBadge({ status }: { status: ReferralStatus }) {
  const meta = STATUS_META[status];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide ${meta.className}`}
    >
      {meta.label}
    </span>
  );
}

// ── Urgency ────────────────────────────────────────────────────────────────────

const URGENCY_META: Record<ReferralUrgency, { label: string; className: string }> = {
  ROUTINE: { label: "Routine", className: "bg-canvas text-muted" },
  URGENT: { label: "Urgent", className: "bg-orange-50 text-orange-700" },
  EMERGENT: { label: "Emergent", className: "bg-red-50 text-red-700" },
};

export function ReferralUrgencyBadge({ urgency }: { urgency: ReferralUrgency }) {
  const meta = URGENCY_META[urgency];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide ${meta.className}`}
    >
      {meta.label}
    </span>
  );
}

// ── Delivery method ────────────────────────────────────────────────────────────

const DELIVERY_LABELS: Record<ReferralDeliveryMethod, string> = {
  OCEAN_EREFERRAL: "Ocean eReferral",
  PDF_FAX: "PDF / Fax",
  PDF_PRINT: "PDF / Print",
};

export function deliveryLabel(method: ReferralDeliveryMethod): string {
  return DELIVERY_LABELS[method];
}
