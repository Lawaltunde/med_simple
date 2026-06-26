import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

const WEEKDAYS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];
const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

function startOfDay(d: Date): Date {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/**
 * A compact, dependency-free month-grid calendar. Hand-rolled rather than pulling in a heavy
 * calendar library so it matches the design system.
 *
 * The AI never commits a date: `recommended` shades a *window* (the physician picks the real day),
 * `marked` dots days that already have appointments, and `minDate` disables earlier days for booking.
 */
export function MonthCalendar({
  selected,
  onSelect,
  minDate,
  recommended,
  marked = [],
}: {
  selected: Date | null;
  onSelect: (day: Date) => void;
  minDate?: Date;
  recommended?: { from: Date; to: Date } | null;
  marked?: Date[];
}) {
  const initial = selected ?? recommended?.from ?? new Date();
  const [view, setView] = useState(new Date(initial.getFullYear(), initial.getMonth(), 1));

  const year = view.getFullYear();
  const month = view.getMonth();
  const firstWeekday = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const min = minDate ? startOfDay(minDate) : null;
  const today = startOfDay(new Date());

  const cells: (Date | null)[] = [];
  for (let i = 0; i < firstWeekday; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(year, month, d));
  while (cells.length % 7 !== 0) cells.push(null);

  function inRecommended(d: Date): boolean {
    if (!recommended) return false;
    const t = startOfDay(d).getTime();
    return t >= startOfDay(recommended.from).getTime() && t <= startOfDay(recommended.to).getTime();
  }

  return (
    <div className="rounded-2xl border border-line bg-white p-3">
      <div className="mb-1 flex items-center justify-between px-1">
        <button
          type="button"
          onClick={() => setView(new Date(year, month - 1, 1))}
          className="rounded-lg p-1.5 text-muted transition-colors hover:bg-brand-50 hover:text-brand-700"
          aria-label="Previous month"
        >
          <ChevronLeft className="size-4" />
        </button>
        <span className="text-sm font-semibold text-ink">{MONTHS[month]} {year}</span>
        <button
          type="button"
          onClick={() => setView(new Date(year, month + 1, 1))}
          className="rounded-lg p-1.5 text-muted transition-colors hover:bg-brand-50 hover:text-brand-700"
          aria-label="Next month"
        >
          <ChevronRight className="size-4" />
        </button>
      </div>

      <div className="grid grid-cols-7 px-1 text-center text-[0.65rem] font-semibold uppercase tracking-wide text-muted">
        {WEEKDAYS.map((w) => <div key={w} className="py-1">{w}</div>)}
      </div>

      <div className="grid grid-cols-7 gap-1 p-1">
        {cells.map((d, i) => {
          if (!d) return <div key={i} />;
          const disabled = min ? startOfDay(d).getTime() < min.getTime() : false;
          const isSelected = !!selected && sameDay(d, selected);
          const isToday = sameDay(d, today);
          const rec = inRecommended(d);
          const isMarked = marked.some((m) => sameDay(m, d));
          return (
            <button
              key={i}
              type="button"
              disabled={disabled}
              onClick={() => onSelect(d)}
              aria-label={d.toDateString()}
              className={cn(
                "relative flex h-9 items-center justify-center rounded-lg text-sm transition-colors",
                disabled ? "cursor-not-allowed text-line" : "text-ink hover:bg-brand-50",
                rec && !isSelected && !disabled && "bg-brand-50 text-brand-800 ring-1 ring-inset ring-brand-200",
                isToday && !isSelected && "font-bold text-brand-700",
                isSelected && "bg-brand-600 font-semibold text-white hover:bg-brand-600",
              )}
            >
              {d.getDate()}
              {isMarked && !isSelected && (
                <span className="absolute bottom-1 size-1 rounded-full bg-brand-500" />
              )}
            </button>
          );
        })}
      </div>

      {recommended && (
        <p className="flex items-center gap-1.5 px-1 pb-1 pt-2 text-xs text-muted">
          <span className="inline-block size-2.5 rounded-sm bg-brand-50 ring-1 ring-inset ring-brand-200" />
          AI-recommended follow-up window — pick the day that fits
        </p>
      )}
    </div>
  );
}
