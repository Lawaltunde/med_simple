import { Logo } from "./Logo";

export function Footer() {
  return (
    <footer className="border-t border-line bg-white">
      <div className="mx-auto max-w-6xl px-5 py-12 lg:px-8">
        <div className="flex flex-col gap-8 md:flex-row md:items-start md:justify-between">
          <div className="max-w-xs">
            <Logo />
            <p className="mt-3 text-sm leading-relaxed text-muted">
              Your family's health assistant. Text it. Photograph it. Speak it. We'll take it from there.
            </p>
          </div>
          <nav className="grid grid-cols-2 gap-x-12 gap-y-2 text-sm sm:grid-cols-3" aria-label="Footer">
            <a href="#how" className="text-muted hover:text-brand-700">How it works</a>
            <a href="#features" className="text-muted hover:text-brand-700">Features</a>
            <a href="#trust" className="text-muted hover:text-brand-700">Privacy</a>
            <a href="/login" className="text-muted hover:text-brand-700">Log in</a>
            <a href="/signup" className="text-muted hover:text-brand-700">Get started</a>
          </nav>
        </div>
        <div className="mt-10 flex flex-col gap-2 border-t border-line pt-6 text-xs text-muted sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} MedSimple. For Canadian primary care.</p>
          <p>PHIPA &amp; PIPEDA aligned · All clinical decisions are made by a physician.</p>
        </div>
      </div>
    </footer>
  );
}
