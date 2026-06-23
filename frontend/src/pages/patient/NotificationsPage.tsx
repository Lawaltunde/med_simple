import { Loader2, Bell, CheckCheck } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { useNotifications, useMarkRead, useMarkAllRead } from "@/features/notifications/hooks";
import { formatDateTime } from "@/features/symptom/display";
import { cn } from "@/lib/utils";

export function NotificationsPage() {
  const { user } = useAuth();
  const { data: items, isLoading } = useNotifications(user?.id);
  const markRead = useMarkRead(user?.id ?? "");
  const markAllRead = useMarkAllRead(user?.id ?? "");
  const hasUnread = items?.some((n) => !n.read) ?? false;

  return (
    <div className="mx-auto max-w-2xl">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Notifications</h2>
          <p className="mt-1 text-muted">Updates about your care, refreshed automatically.</p>
        </div>
        {hasUnread && (
          <button
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending}
            className="inline-flex items-center gap-1.5 rounded-full border border-line px-3 py-1.5 text-sm font-medium text-muted hover:bg-brand-50 hover:text-ink"
          >
            <CheckCheck className="size-4" /> Mark all read
          </button>
        )}
      </header>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : !items || items.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-line bg-white px-6 py-16 text-center">
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Bell className="size-6" />
          </span>
          <p className="mt-5 font-medium text-ink">You're all caught up</p>
          <p className="mt-1 text-sm text-muted">Updates about your symptom checks and care plans land here.</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {items.map((n) => (
            <li key={n.id}>
              <button
                onClick={() => !n.read && markRead.mutate(n.id)}
                className={cn(
                  "flex w-full gap-3 rounded-2xl border p-4 text-left transition-colors",
                  n.read ? "border-line bg-white" : "border-brand-200 bg-brand-50/50 hover:bg-brand-50"
                )}
              >
                {!n.read && <span className="mt-1.5 size-2 shrink-0 rounded-full bg-brand-600" aria-label="unread" />}
                <div className={cn("min-w-0 flex-1", n.read && "pl-5")}>
                  <p className={cn("text-[0.95rem]", n.read ? "font-medium text-ink" : "font-bold text-brand-900")}>
                    {n.title}
                  </p>
                  <p className="mt-0.5 text-sm text-muted">{n.body}</p>
                  <p className="mt-1 text-xs text-muted">{formatDateTime(n.createdAt)}</p>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
