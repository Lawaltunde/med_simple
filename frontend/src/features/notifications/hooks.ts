import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listNotifications, markAllRead, markRead, unreadCount } from "./api";

// Near-real-time without WebSockets: poll on an interval. React Query pauses polling
// while the tab is hidden, so this is cheap. The badge polls faster than the full feed.
const BADGE_POLL_MS = 20_000;
const FEED_POLL_MS = 30_000;

export const notificationKeys = {
  feed: (userId: string) => ["notifications", "feed", userId] as const,
  unread: (userId: string) => ["notifications", "unread", userId] as const,
};

export function useUnreadCount(userId: string | undefined) {
  return useQuery({
    queryKey: notificationKeys.unread(userId ?? ""),
    queryFn: () => unreadCount(userId!),
    enabled: !!userId,
    refetchInterval: BADGE_POLL_MS,
    select: (d) => d.unreadCount,
  });
}

export function useNotifications(userId: string | undefined) {
  return useQuery({
    queryKey: notificationKeys.feed(userId ?? ""),
    queryFn: () => listNotifications(userId!),
    enabled: !!userId,
    refetchInterval: FEED_POLL_MS,
  });
}

function useInvalidate(userId: string) {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: notificationKeys.feed(userId) });
    qc.invalidateQueries({ queryKey: notificationKeys.unread(userId) });
  };
}

export function useMarkRead(userId: string) {
  const invalidate = useInvalidate(userId);
  return useMutation({
    mutationFn: (notificationId: string) => markRead(userId, notificationId),
    onSuccess: invalidate,
  });
}

export function useMarkAllRead(userId: string) {
  const invalidate = useInvalidate(userId);
  return useMutation({
    mutationFn: () => markAllRead(userId),
    onSuccess: invalidate,
  });
}
