import { useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { ChevronsRight, Menu, X, LogOut, type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Logo } from "./Logo";
import { useAuth } from "@/auth/AuthProvider";

export interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
  end?: boolean;
}

// Reusable authenticated shell. Patient app and clinician console pass their own
// nav config; the layout (collapsible sidebar + mobile drawer + topbar) is shared.
export function AppShell({ nav, title }: { nav: NavItem[]; title: string }) {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout } = useAuth();
  const location = useLocation();

  const current = nav.find((n) => (n.end ? location.pathname === n.to : location.pathname.startsWith(n.to)));

  return (
    <div className="flex min-h-dvh bg-canvas">
      {/* ── Desktop sidebar ─────────────────────────────── */}
      <aside
        className={cn(
          "sticky top-0 hidden h-dvh shrink-0 flex-col border-r border-line bg-white transition-[width] duration-300 lg:flex",
          collapsed ? "w-[4.5rem]" : "w-64"
        )}
      >
        <SidebarBody nav={nav} collapsed={collapsed} user={user} onLogout={logout} />
        <button
          onClick={() => setCollapsed((v) => !v)}
          className="flex items-center gap-3 border-t border-line p-3 text-muted transition-colors hover:bg-brand-50 hover:text-ink"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <span className="grid size-9 place-content-center">
            <ChevronsRight className={cn("size-5 transition-transform", !collapsed && "rotate-180")} />
          </span>
          {!collapsed && <span className="text-sm font-medium">Collapse</span>}
        </button>
      </aside>

      {/* ── Mobile drawer ───────────────────────────────── */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-brand-950/40" onClick={() => setMobileOpen(false)} aria-hidden />
          <aside className="absolute left-0 top-0 flex h-full w-72 flex-col bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-line p-4">
              <Logo />
              <button onClick={() => setMobileOpen(false)} aria-label="Close menu" className="rounded-lg p-1.5 text-muted hover:bg-brand-50">
                <X className="size-6" />
              </button>
            </div>
            <SidebarBody nav={nav} collapsed={false} user={user} onLogout={logout} onNavigate={() => setMobileOpen(false)} hideHeader />
          </aside>
        </div>
      )}

      {/* ── Main column ─────────────────────────────────── */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-line bg-white/85 px-4 py-3 backdrop-blur-md lg:px-8">
          <button
            onClick={() => setMobileOpen(true)}
            className="rounded-lg p-1.5 text-ink hover:bg-brand-50 lg:hidden"
            aria-label="Open menu"
          >
            <Menu className="size-6" />
          </button>
          <h1 className="text-lg font-bold text-brand-900">{current?.label ?? title}</h1>
        </header>

        <main className="flex-1 px-4 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function SidebarBody({
  nav,
  collapsed,
  user,
  onLogout,
  onNavigate,
  hideHeader,
}: {
  nav: NavItem[];
  collapsed: boolean;
  user: { firstName: string; lastName: string; role: string } | null;
  onLogout: () => void;
  onNavigate?: () => void;
  hideHeader?: boolean;
}) {
  return (
    <>
      {!hideHeader && (
        <div className="flex h-[3.75rem] items-center border-b border-line px-4">
          <Logo showWordmark={!collapsed} />
        </div>
      )}

      <nav className="flex-1 space-y-1 overflow-y-auto p-2">
        {nav.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                "relative flex h-11 items-center rounded-xl transition-colors",
                isActive
                  ? "bg-brand-50 font-semibold text-brand-700"
                  : "text-muted hover:bg-brand-50/60 hover:text-ink"
              )
            }
          >
            {({ isActive }) => (
              <>
                {isActive && <span className="absolute left-0 h-6 w-1 rounded-r-full bg-brand-600" />}
                <span className="grid w-[3.25rem] shrink-0 place-content-center">
                  <item.icon className="size-5" />
                </span>
                {!collapsed && <span className="text-sm">{item.label}</span>}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-line p-2">
        <div className={cn("flex items-center gap-3 rounded-xl px-2 py-2", collapsed && "justify-center")}>
          <span className="grid size-9 shrink-0 place-content-center rounded-full bg-brand-600 text-sm font-bold text-white">
            {user ? `${user.firstName[0] ?? ""}${user.lastName[0] ?? ""}` : "?"}
          </span>
          {!collapsed && user && (
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-ink">
                {user.firstName} {user.lastName}
              </p>
              <p className="truncate text-xs capitalize text-muted">{user.role.toLowerCase().replace("_", " ")}</p>
            </div>
          )}
          <button
            onClick={onLogout}
            className="rounded-lg p-1.5 text-muted hover:bg-brand-50 hover:text-ink"
            aria-label="Log out"
            title="Log out"
          >
            <LogOut className="size-5" />
          </button>
        </div>
      </div>
    </>
  );
}
