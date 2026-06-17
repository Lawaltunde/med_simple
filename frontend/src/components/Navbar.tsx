import { useState } from "react";
import { Link } from "react-router-dom";
import { Menu, X } from "lucide-react";
import { Logo } from "./Logo";
import { buttonVariants } from "./ui/button";

const links = [
  { label: "How it works", href: "#how" },
  { label: "Features", href: "#features" },
  { label: "Trust & privacy", href: "#trust" },
];

export function Navbar() {
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 w-full border-b border-line bg-white/85 backdrop-blur-md">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3.5 lg:px-8">
        <Link to="/" aria-label="MedSimple home" className="rounded-lg">
          <Logo />
        </Link>

        <div className="hidden items-center gap-8 lg:flex">
          <ul className="flex items-center gap-8 text-[0.95rem] font-medium text-muted">
            {links.map((l) => (
              <li key={l.href}>
                <a href={l.href} className="rounded-md transition-colors hover:text-brand-700">
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
          <div className="flex items-center gap-2.5">
            <Link to="/login" className={buttonVariants({ variant: "ghost", size: "sm" })}>
              Log in
            </Link>
            <Link to="/signup" className={buttonVariants({ size: "sm" })}>
              Get started
            </Link>
          </div>
        </div>

        <button
          onClick={() => setOpen((v) => !v)}
          className="rounded-lg p-2 text-ink lg:hidden"
          aria-label={open ? "Close menu" : "Open menu"}
          aria-expanded={open}
        >
          {open ? <X className="size-6" /> : <Menu className="size-6" />}
        </button>
      </nav>

      {open && (
        <div className="border-t border-line bg-white px-5 py-4 lg:hidden">
          <ul className="flex flex-col gap-1">
            {links.map((l) => (
              <li key={l.href}>
                <a
                  href={l.href}
                  onClick={() => setOpen(false)}
                  className="block rounded-lg px-3 py-2.5 font-medium text-ink hover:bg-brand-50"
                >
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
          <div className="mt-3 flex flex-col gap-2">
            <Link to="/login" className={buttonVariants({ variant: "outline" })}>
              Log in
            </Link>
            <Link to="/signup" className={buttonVariants({})}>
              Get started
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
