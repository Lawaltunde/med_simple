import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  MessageSquareText,
  Camera,
  Mic,
  ShieldCheck,
  Stethoscope,
  ArrowRight,
  Activity,
} from "lucide-react";
import { Navbar } from "@/components/Navbar";
import { Footer } from "@/components/Footer";
import { buttonVariants } from "@/components/ui/button";

const fade = {
  initial: { opacity: 0, y: 14 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true, margin: "-80px" },
  transition: { duration: 0.5, ease: "easeOut" },
} as const;

const features = [
  {
    icon: MessageSquareText,
    title: "Text it",
    body: "Describe what's wrong in your own words. No forms, no jargon — just type and send.",
  },
  {
    icon: Camera,
    title: "Photograph it",
    body: "A rash, a cut, a swollen joint. Snap a photo and our AI helps your clinician see it clearly.",
  },
  {
    icon: Mic,
    title: "Speak it",
    body: "Hands full or not feeling well? Record a voice note and we'll transcribe the rest.",
  },
];

const steps = [
  { n: "1", title: "Tell us what's wrong", body: "Text, photo, or voice — whatever's easiest in the moment." },
  { n: "2", title: "AI sorts the urgency", body: "Your symptoms are triaged in seconds, so the right care comes first." },
  { n: "3", title: "A physician takes over", body: "Every plan, prescription, and result is reviewed and released by a doctor." },
];

export function LandingPage() {
  return (
    <div className="min-h-dvh">
      <Navbar />

      {/* ── Hero ───────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 -z-10 bg-gradient-to-b from-brand-50 to-canvas"
        />
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-5 py-16 lg:grid-cols-2 lg:gap-8 lg:px-8 lg:py-24">
          <motion.div {...fade}>
            <span className="inline-flex items-center gap-2 rounded-full bg-brand-100 px-3.5 py-1.5 text-sm font-medium text-brand-700">
              <Activity className="size-4" /> Physician-reviewed virtual care
            </span>
            <h1 className="mt-5 text-4xl font-extrabold leading-[1.08] tracking-tight text-brand-900 sm:text-5xl lg:text-6xl">
              Your family's<br />health assistant.
            </h1>
            <p className="mt-5 max-w-md text-lg leading-relaxed text-muted">
              Text it. Photograph it. Speak it. MedSimple understands your symptoms, sorts the urgency,
              and connects you to a physician who decides what happens next.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link to="/signup" className={buttonVariants({ size: "lg" })}>
                Get started — it's free <ArrowRight className="size-5" />
              </Link>
              <a href="#how" className={buttonVariants({ variant: "outline", size: "lg" })}>
                See how it works
              </a>
            </div>
            <p className="mt-4 text-sm text-muted">
              No wait rooms. Every decision made by a licensed doctor.
            </p>
          </motion.div>

          {/* Hero visual — composed card, not a heavy image */}
          <motion.div
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.6, ease: "easeOut" }}
            className="relative"
          >
            <div className="rounded-2xl bg-gradient-to-br from-brand-600 to-brand-800 p-6 shadow-2xl shadow-brand-900/20 sm:p-8">
              <div className="flex items-center gap-2 text-brand-200">
                <Stethoscope className="size-5" />
                <span className="text-sm font-medium">Live triage</span>
              </div>
              {/* patient chip */}
              <div className="mt-5 ml-auto max-w-[80%] rounded-2xl rounded-tr-sm bg-white px-4 py-3 shadow-lg">
                <p className="text-sm font-semibold text-brand-900">"Chest feels tight and I'm short of breath."</p>
                <p className="mt-0.5 text-xs text-muted">You · just now</p>
              </div>
              {/* AI chip */}
              <div className="mt-3 max-w-[85%] rounded-2xl rounded-tl-sm bg-brand-950 px-4 py-3">
                <div className="flex items-center gap-2">
                  <span className="inline-flex size-5 items-center justify-center rounded-full bg-[--color-urgency-emergency]/20">
                    <span className="size-2 rounded-full bg-[--color-urgency-emergency]" />
                  </span>
                  <span className="text-xs font-bold uppercase tracking-wide text-[#ff8b8b]">Emergency</span>
                </div>
                <p className="mt-1.5 text-sm font-medium text-white">
                  These symptoms may be urgent. Call 911 or go to the nearest emergency department now.
                </p>
              </div>
              {/* pulse line */}
              <svg viewBox="0 0 320 40" className="mt-6 w-full" aria-hidden>
                <path
                  d="M0 20 H120 L132 6 L150 34 L164 20 H320"
                  fill="none"
                  stroke="#7FE3C4"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ── Features: three ways to ask ────────────────────── */}
      <section id="features" className="mx-auto max-w-6xl px-5 py-16 lg:px-8 lg:py-24">
        <motion.div {...fade} className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-extrabold tracking-tight text-brand-900 sm:text-4xl">
            One assistant. Three ways to ask.
          </h2>
          <p className="mt-4 text-lg text-muted">
            However you're most comfortable explaining what's wrong — MedSimple understands it.
          </p>
        </motion.div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              {...fade}
              transition={{ duration: 0.5, ease: "easeOut", delay: i * 0.08 }}
              className="rounded-2xl border border-line bg-white p-7 shadow-sm transition-shadow hover:shadow-md"
            >
              <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                <f.icon className="size-6" />
              </span>
              <h3 className="mt-5 text-xl font-bold text-brand-900">{f.title}</h3>
              <p className="mt-2 leading-relaxed text-muted">{f.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ── How it works ───────────────────────────────────── */}
      <section id="how" className="border-y border-line bg-brand-50/60">
        <div className="mx-auto max-w-6xl px-5 py-16 lg:px-8 lg:py-24">
          <motion.h2 {...fade} className="text-center text-3xl font-extrabold tracking-tight text-brand-900 sm:text-4xl">
            From symptom to care, in three steps.
          </motion.h2>
          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {steps.map((s, i) => (
              <motion.div
                key={s.n}
                {...fade}
                transition={{ duration: 0.5, ease: "easeOut", delay: i * 0.08 }}
                className="relative"
              >
                <div className="flex size-12 items-center justify-center rounded-full bg-brand-600 text-lg font-bold text-white">
                  {s.n}
                </div>
                <h3 className="mt-5 text-xl font-bold text-brand-900">{s.title}</h3>
                <p className="mt-2 leading-relaxed text-muted">{s.body}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Trust strip ────────────────────────────────────── */}
      <section id="trust" className="mx-auto max-w-6xl px-5 py-16 lg:px-8 lg:py-24">
        <div className="grid gap-6 sm:grid-cols-3">
          {[
            { icon: ShieldCheck, title: "Private by design", body: "PHIPA & PIPEDA aligned. Your health data is encrypted and never sold." },
            { icon: Stethoscope, title: "Always a real doctor", body: "AI assists — but every diagnosis, prescription, and result is physician-reviewed." },
            { icon: Activity, title: "Urgency first", body: "Care is ordered by clinical need, not by who booked first." },
          ].map((t) => (
            <motion.div key={t.title} {...fade} className="flex gap-4 rounded-2xl bg-white p-6 ring-1 ring-line">
              <t.icon className="size-7 shrink-0 text-brand-600" />
              <div>
                <h3 className="font-bold text-brand-900">{t.title}</h3>
                <p className="mt-1 text-sm leading-relaxed text-muted">{t.body}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ── CTA ────────────────────────────────────────────── */}
      <section className="px-5 pb-20 lg:px-8">
        <motion.div
          {...fade}
          className="mx-auto max-w-5xl overflow-hidden rounded-2xl bg-gradient-to-br from-brand-600 to-brand-800 px-8 py-14 text-center shadow-xl"
        >
          <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
            Health questions shouldn't wait.
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-lg text-brand-100">
            Create your free account and get a physician-reviewed answer to what's worrying you.
          </p>
          <div className="mt-8 flex justify-center">
            <Link
              to="/signup"
              className="inline-flex h-14 items-center justify-center gap-2 rounded-full bg-white px-8 text-base font-semibold text-brand-700 transition-colors hover:bg-brand-50"
            >
              Get started free <ArrowRight className="size-5" />
            </Link>
          </div>
        </motion.div>
      </section>

      <Footer />
    </div>
  );
}
