import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NotificationsPage } from "./NotificationsPage";
import type { AppNotification } from "@/features/notifications/api";

// Authenticated patient.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

// Stub the notifications network layer.
const listMock = vi.fn();
const markReadMock = vi.fn();
const markAllReadMock = vi.fn();
vi.mock("@/features/notifications/api", () => ({
  listNotifications: (...a: unknown[]) => listMock(...a),
  unreadCount: () => Promise.resolve({ unreadCount: 0 }),
  markRead: (...a: unknown[]) => markReadMock(...a),
  markAllRead: (...a: unknown[]) => markAllReadMock(...a),
}));

function notif(over: Partial<AppNotification>): AppNotification {
  return {
    id: "n-1",
    type: "CARE_PLAN_UPDATE",
    title: "Care plan updated",
    body: "Your physician added instructions.",
    read: false,
    readAt: null,
    createdAt: "2026-06-20T10:00:00Z",
    ...over,
  };
}

function renderScreen() {
  const qc = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <NotificationsPage />
    </QueryClientProvider>
  );
}

describe("NotificationsPage", () => {
  beforeEach(() => {
    listMock.mockReset();
    markReadMock.mockReset();
    markAllReadMock.mockReset();
  });

  it("shows the empty state when there are no notifications", async () => {
    listMock.mockResolvedValue([]);
    renderScreen();
    expect(await screen.findByText(/all caught up/i)).toBeInTheDocument();
  });

  it("renders the feed and marks one read on click", async () => {
    const unread = notif({ id: "n-1", title: "Result ready", read: false });
    listMock.mockResolvedValue([unread]);
    markReadMock.mockResolvedValueOnce({ ...unread, read: true, readAt: "2026-06-20T11:00:00Z" });

    const user = userEvent.setup();
    renderScreen();

    await user.click(await screen.findByText("Result ready"));
    await waitFor(() => expect(markReadMock).toHaveBeenCalledWith("patient-1", "n-1"));
  });

  it("does not re-mark an already-read notification", async () => {
    listMock.mockResolvedValue([notif({ id: "n-2", title: "Old news", read: true })]);
    const user = userEvent.setup();
    renderScreen();

    await user.click(await screen.findByText("Old news"));
    expect(markReadMock).not.toHaveBeenCalled();
  });

  it("offers mark-all-read only when something is unread, and calls it", async () => {
    listMock.mockResolvedValue([notif({ id: "n-3", read: false })]);
    markAllReadMock.mockResolvedValueOnce({ marked: 1 });
    const user = userEvent.setup();
    renderScreen();

    const btn = await screen.findByRole("button", { name: /mark all read/i });
    await user.click(btn);
    await waitFor(() => expect(markAllReadMock).toHaveBeenCalledWith("patient-1"));
  });

  it("hides mark-all-read when everything is already read", async () => {
    listMock.mockResolvedValue([notif({ id: "n-4", read: true })]);
    renderScreen();
    await screen.findByText("Care plan updated");
    expect(screen.queryByRole("button", { name: /mark all read/i })).not.toBeInTheDocument();
  });
});
