import { Link } from "react-router-dom";
import { PlusCircle, MessageSquareText, Camera, Mic, ArrowRight } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { buttonVariants } from "@/components/ui/button";

export function PatientHome() {
  const { user } = useAuth();

  return (
    <div className="mx-auto max-w-4xl space-y-8">
      {/* Greeting + primary CTA */}
      <section className="overflow-hidden rounded-2xl bg-gradient-to-br from-brand-600 to-brand-800 p-7 text-white sm:p-9">
        <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
          Hi {user?.firstName} — how are you feeling?
        </h2>
        <p className="mt-2 max-w-md text-brand-100">
          Tell us what's wrong and we'll sort the urgency. A physician reviews every answer.
        </p>
        <Link
          to="/app/new"
          className="mt-6 inline-flex h-12 items-center gap-2 rounded-full bg-white px-6 font-semibold text-brand-700 transition-colors hover:bg-brand-50"
        >
          <PlusCircle className="size-5" /> Start a new symptom check
        </Link>
      </section>

      {/* Three ways to ask */}
      <section>
        <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-muted">Three ways to ask</h3>
        <div className="grid gap-4 sm:grid-cols-3">
          {[
            { icon: MessageSquareText, title: "Text it", body: "Describe it in your own words." },
            { icon: Camera, title: "Photograph it", body: "Snap a rash, cut, or swelling." },
            { icon: Mic, title: "Speak it", body: "Record a voice note instead." },
          ].map((c) => (
            <div key={c.title} className="rounded-2xl border border-line bg-white p-5">
              <span className="inline-flex size-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                <c.icon className="size-5" />
              </span>
              <h4 className="mt-4 font-bold text-brand-900">{c.title}</h4>
              <p className="mt-1 text-sm text-muted">{c.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Recent activity placeholder — wired to the API in the next step */}
      <section className="rounded-2xl border border-line bg-white p-6">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-brand-900">Your recent episodes</h3>
          <Link to="/app/new" className={buttonVariants({ variant: "ghost", size: "sm" })}>
            New <ArrowRight className="size-4" />
          </Link>
        </div>
        <div className="mt-6 flex flex-col items-center justify-center rounded-xl bg-canvas py-12 text-center">
          <p className="font-medium text-ink">No symptom checks yet</p>
          <p className="mt-1 max-w-xs text-sm text-muted">
            When you start a symptom check, it'll appear here with its urgency and status.
          </p>
        </div>
      </section>
    </div>
  );
}
