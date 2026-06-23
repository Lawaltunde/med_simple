import { api } from "@/lib/api";

export type NotificationType =
  | "EMERGENCY_ALERT"
  | "URGENCY_UPDATED"
  | "PHYSICIAN_ASSIGNED"
  | "FOLLOW_UP_RECEIVED"
  | "FOLLOW_UP_PROMPT"
  | "PRESCRIPTION_SENT"
  | "PRESCRIPTION_FILLED"
  | "REQUISITION_BOOKED"
  | "APPOINTMENT_REMINDER"
  | "RESULT_READY"
  | "CARE_PLAN_UPDATE"
  | "SYMPTOM_UPDATE_PROMPT"
  | "DETERIORATION_ALERT"
  | "REFERRAL_UPDATE";

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

const base = (userId: string) => `/users/${userId}/notifications`;

export function listNotifications(userId: string): Promise<AppNotification[]> {
  return api<AppNotification[]>(base(userId));
}

export function unreadCount(userId: string): Promise<{ unreadCount: number }> {
  return api<{ unreadCount: number }>(`${base(userId)}/unread-count`);
}

export function markRead(userId: string, notificationId: string): Promise<AppNotification> {
  return api<AppNotification>(`${base(userId)}/${notificationId}/read`, { method: "POST" });
}

export function markAllRead(userId: string): Promise<{ marked: number }> {
  return api<{ marked: number }>(`${base(userId)}/read-all`, { method: "POST" });
}
