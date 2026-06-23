import type { CarePlanStatus } from "./types";

const STATUS: Record<CarePlanStatus, { label: string; className: string }> = {
  ACTIVE: { label: "Active", className: "bg-brand-100 text-brand-700" },
  COMPLETED: { label: "Completed", className: "bg-canvas text-muted" },
  CANCELLED: { label: "Cancelled", className: "bg-canvas text-muted line-through" },
};

export function CarePlanStatusBadge({ status }: { status: CarePlanStatus }) {
  const meta = STATUS[status];
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide ${meta.className}`}>
      {meta.label}
    </span>
  );
}
