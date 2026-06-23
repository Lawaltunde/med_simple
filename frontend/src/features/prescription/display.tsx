import type { DeliveryMethod, PrescriptionStatus } from "./types";

const STATUS_META: Record<PrescriptionStatus, { label: string; className: string }> = {
  DRAFT:           { label: "Draft",     className: "bg-canvas text-muted" },
  SIGNED:          { label: "Signed",    className: "bg-brand-100 text-brand-700" },
  SENT_ELECTRONIC: { label: "Sent (e-Rx)", className: "bg-brand-100 text-brand-700" },
  SENT_PDF:        { label: "Sent (PDF)", className: "bg-brand-100 text-brand-700" },
  FILLED:          { label: "Filled",    className: "bg-green-100 text-green-800" },
  CANCELLED:       { label: "Cancelled", className: "bg-canvas text-muted line-through" },
};

const DELIVERY_META: Record<DeliveryMethod, string> = {
  PRESCRIBIT: "PrescribeIT (electronic)",
  PDF_FAX:    "PDF — fax",
  PDF_PRINT:  "PDF — print",
};

export function PrescriptionStatusBadge({ status }: { status: PrescriptionStatus }) {
  const meta = STATUS_META[status];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide ${meta.className}`}
    >
      {meta.label}
    </span>
  );
}

export function deliveryLabel(method: DeliveryMethod | null): string {
  if (!method) return "—";
  return DELIVERY_META[method] ?? method;
}
