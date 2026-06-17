import { Link } from "react-router-dom";
import { Hammer, ArrowLeft } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";

// Placeholder for patient sections that are navigable but not built yet. Keeps the user
// inside the app shell (still signed in) instead of falling through to the landing page.
export function ComingSoon({ title, description }: { title: string; description?: string }) {
  return (
    <div className="mx-auto flex max-w-lg flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
      <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
        <Hammer className="size-6" />
      </span>
      <h2 className="mt-5 text-xl font-extrabold tracking-tight text-brand-900">{title} are coming soon</h2>
      <p className="mt-2 text-sm text-muted">
        {description ?? "We're still building this section. It'll show up here once it's ready."}
      </p>
      <Link to="/app" className={buttonVariants({ variant: "outline", className: "mt-6" })}>
        <ArrowLeft className="size-4" /> Back to home
      </Link>
    </div>
  );
}
