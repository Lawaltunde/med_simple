import * as React from "react";
import { cn } from "@/lib/utils";

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  hint?: string;
  error?: string;
}

// Multi-line counterpart to <Input>, same solid high-contrast treatment.
export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, hint, error, id, ...props }, ref) => {
    const fieldId = id ?? props.name;
    return (
      <div className="w-full">
        {label && (
          <label htmlFor={fieldId} className="mb-1.5 block text-sm font-medium text-ink">
            {label}
          </label>
        )}
        <textarea
          id={fieldId}
          ref={ref}
          aria-invalid={!!error}
          aria-describedby={error ? `${fieldId}-error` : hint ? `${fieldId}-hint` : undefined}
          className={cn(
            "min-h-32 w-full resize-y rounded-xl border border-line bg-white px-4 py-3 text-[0.95rem] text-ink shadow-sm transition-colors",
            "placeholder:text-muted/70 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25",
            error && "border-[--color-urgency-emergency] focus:border-[--color-urgency-emergency] focus:ring-[--color-urgency-emergency]/20",
            className
          )}
          {...props}
        />
        {error ? (
          <p id={`${fieldId}-error`} className="mt-1.5 text-sm text-[--color-urgency-emergency]">
            {error}
          </p>
        ) : hint ? (
          <p id={`${fieldId}-hint`} className="mt-1.5 text-sm text-muted">
            {hint}
          </p>
        ) : null}
      </div>
    );
  }
);
Textarea.displayName = "Textarea";
