import { cn } from "@/lib/utils";

// Inline brand mark — heartbeat pulse in the rounded green tile + wordmark.
// Inline (not the SVG file) so it scales crisply and inherits currentColor where needed.
export function Logo({ className, showWordmark = true }: { className?: string; showWordmark?: boolean }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)} aria-label="MedSimple">
      <svg viewBox="0 0 56 56" className="h-9 w-9 shrink-0" role="img" aria-hidden="true">
        <defs>
          <linearGradient id="ms-tile" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#1AA079" />
            <stop offset="1" stopColor="#0E5C49" />
          </linearGradient>
        </defs>
        <rect width="56" height="56" rx="16" fill="url(#ms-tile)" />
        <path
          d="M11 29 H22 L26 17 L31 41 L36 23 L39 29 H46"
          fill="none"
          stroke="#fff"
          strokeWidth="3.4"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx="46" cy="29" r="2.4" fill="#7FE3C4" />
      </svg>
      {showWordmark && (
        <span className="text-[1.35rem] font-extrabold tracking-tight">
          <span className="text-brand-900">Med</span>
          <span className="text-brand-600">Simple</span>
        </span>
      )}
    </span>
  );
}
