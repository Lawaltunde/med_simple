import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PrivacyPage } from "./PrivacyPage";
import type { Consent } from "@/features/consent/api";

vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

const listMock = vi.fn();
const recordMock = vi.fn();
vi.mock("@/features/consent/api", () => ({
  listConsents: (...a: unknown[]) => listMock(...a),
  recordConsent: (...a: unknown[]) => recordMock(...a),
}));

function consent(over: Partial<Consent>): Consent {
  return {
    id: "c-1", consentType: "AI_TRAINING", version: "v1.0",
    grantedAt: null, revokedAt: null, createdAt: "2026-06-23T10:00:00Z", ...over,
  };
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <PrivacyPage />
    </QueryClientProvider>
  );
}

describe("PrivacyPage", () => {
  beforeEach(() => {
    listMock.mockReset();
    recordMock.mockReset().mockResolvedValue(consent({ grantedAt: "2026-06-23T11:00:00Z" }));
  });

  it("shows the toggle OFF when there is no active AI_TRAINING consent", async () => {
    listMock.mockResolvedValue([]);
    renderPage();
    const sw = await screen.findByRole("switch");
    expect(sw).toHaveAttribute("aria-checked", "false");
  });

  it("shows ON when the latest event is a grant", async () => {
    listMock.mockResolvedValue([consent({ grantedAt: "2026-06-23T11:00:00Z" })]);
    renderPage();
    const sw = await screen.findByRole("switch");
    expect(sw).toHaveAttribute("aria-checked", "true");
  });

  it("shows OFF when the latest event is a revoke", async () => {
    listMock.mockResolvedValue([
      consent({ grantedAt: "2026-06-23T09:00:00Z", revokedAt: "2026-06-23T12:00:00Z" }),
    ]);
    renderPage();
    const sw = await screen.findByRole("switch");
    expect(sw).toHaveAttribute("aria-checked", "false");
  });

  it("grants consent when toggled on", async () => {
    listMock.mockResolvedValue([]);
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("switch"));
    await waitFor(() => expect(recordMock).toHaveBeenCalled());
    const [, body] = recordMock.mock.calls[0];
    expect(body.consentType).toBe("AI_TRAINING");
    expect(body.grantedAt).toBeTruthy();
    expect(body.revokedAt).toBeUndefined();
  });

  it("revokes consent when toggled off", async () => {
    listMock.mockResolvedValue([consent({ grantedAt: "2026-06-23T11:00:00Z" })]);
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("switch"));
    await waitFor(() => expect(recordMock).toHaveBeenCalled());
    const [, body] = recordMock.mock.calls[0];
    expect(body.revokedAt).toBeTruthy();
    expect(body.grantedAt).toBeUndefined();
  });
});
