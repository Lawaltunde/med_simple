import { Link } from "react-router-dom";
import { ShieldCheck, Stethoscope } from "lucide-react";
import { Logo } from "./Logo";

// Split-screen auth shell: form on the left, brand/trust panel on the right.
// The panel collapses on mobile so the form gets the full screen.
export function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-dvh">
      {/* Form side */}
      <div className="flex flex-1 flex-col px-5 py-8 sm:px-10">
        <Link to="/" aria-label="MedSimple home" className="inline-flex w-fit rounded-lg">
          <Logo />
        </Link>
        <div className="flex flex-1 items-center justify-center py-10">
          <div className="w-full max-w-md">{children}</div>
        </div>
      </div>

      {/* Brand panel — hidden on small screens */}
      <div className="relative hidden w-[46%] shrink-0 overflow-hidden bg-gradient-to-br from-brand-600 to-brand-900 lg:block">
        <div aria-hidden className="absolute inset-0">
          <div className="absolute -right-20 top-10 size-80 rounded-full border border-white/10" />
          <div className="absolute right-10 top-40 size-56 rounded-full border border-white/10" />
          <svg viewBox="0 0 400 60" className="absolute bottom-24 left-0 w-full opacity-40" aria-hidden>
            <path
              d="M0 30 H150 L165 8 L185 52 L200 30 H400"
              fill="none"
              stroke="#7FE3C4"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>

        <div className="relative flex h-full flex-col justify-between p-12">
          <div />
          <div>
            <h2 className="text-3xl font-extrabold leading-tight tracking-tight text-white">
              Your family's<br />health assistant.
            </h2>
            <p className="mt-4 max-w-sm text-brand-100">
              Text it. Photograph it. Speak it. MedSimple sorts the urgency and connects you to a
              physician who decides what happens next.
            </p>
          </div>
          <ul className="space-y-3 text-sm text-brand-50">
            <li className="flex items-center gap-3">
              <ShieldCheck className="size-5 shrink-0 text-brand-200" />
              PHIPA &amp; PIPEDA aligned — your data is encrypted and never sold.
            </li>
            <li className="flex items-center gap-3">
              <Stethoscope className="size-5 shrink-0 text-brand-200" />
              Every diagnosis and prescription is reviewed by a real doctor.
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
