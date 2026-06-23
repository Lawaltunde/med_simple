import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReferralsPage } from "./ReferralsPage";
import type { Referral } from "@/features/referral/types";

// Authenticated patient.
vi.mock("@/auth/AuthProvider", () => ({
  useAuth: () => ({ user: { id: "patient-1", firstName: "Jane", lastName: "Doe", role: "PATIENT" } }),
}));

// Stub the network layer.
const getPatientReferralsMock = vi.fn();
vi.mock("@/features/referral/api", () => ({
  getPatientReferrals: (...a: unknown[]) => getPatientReferralsMock(...a),
  getCarePlanReferrals: vi.fn().mockResolvedValue([]),
  createReferral: vi.fn(),
  confirmSpecialty: vi.fn(),
  submitReferral: vi.fn(),
  cancelReferral: vi.fn(),
  getReferral: vi.fn(),
}));

function referral(over: Partial<Referral>): Referral {
  return {
    id: "ref-1",
    carePlanId: "cp-1",
    patientId: "patient-1",
    referringPhysicianId: "doc-1",
    aiSuggestedSpecialty: "Cardiology",
    confirmedSpecialty: "Cardiology",
    clinicalSummary: "Progressive exertional dyspnoea",
    urgency: "ROUTINE",
    status: "SUBMITTED",
    deliveryMethod: "OCEAN_EREFERRAL",
    externalReferralId: "OC-123",
    documentUrl: null,
    submittedAt: "2026-06-01T09:00:00Z",
    createdAt: "2026-05-31T18:00:00Z",
    ...over,
  };
}

function renderScreen() {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <ReferralsPage />
    </QueryClientProvider>
  );
}

describe("ReferralsPage", () => {
  beforeEach(() => getPatientReferralsMock.mockReset());

  it("shows the empty state when there are no referrals", async () => {
    getPatientReferralsMock.mockResolvedValue([]);
    renderScreen();
    expect(await screen.findByText(/no referrals yet/i)).toBeInTheDocument();
  });

  it("renders confirmed specialty and status badge for a referral", async () => {
    getPatientReferralsMock.mockResolvedValue([referral({ confirmedSpecialty: "Cardiology", status: "SUBMITTED" })]);
    renderScreen();
    expect(await screen.findByText("Cardiology")).toBeInTheDocument();
    expect(screen.getByText("Submitted")).toBeInTheDocument();
  });

  it("falls back to aiSuggestedSpecialty when confirmedSpecialty is null", async () => {
    getPatientReferralsMock.mockResolvedValue([
      referral({ confirmedSpecialty: null, aiSuggestedSpecialty: "Neurology" }),
    ]);
    renderScreen();
    expect(await screen.findByText("Neurology")).toBeInTheDocument();
  });

  it("shows ROUTINE urgency badge", async () => {
    getPatientReferralsMock.mockResolvedValue([referral({ urgency: "ROUTINE" })]);
    renderScreen();
    expect(await screen.findByText("Routine")).toBeInTheDocument();
  });

  it("shows URGENT urgency badge", async () => {
    getPatientReferralsMock.mockResolvedValue([referral({ urgency: "URGENT", status: "ACCEPTED" })]);
    renderScreen();
    expect(await screen.findByText("Urgent")).toBeInTheDocument();
    expect(await screen.findByText("Accepted")).toBeInTheDocument();
  });

  it("shows EMERGENT urgency badge", async () => {
    getPatientReferralsMock.mockResolvedValue([referral({ urgency: "EMERGENT" })]);
    renderScreen();
    expect(await screen.findByText("Emergent")).toBeInTheDocument();
  });

  it("shows the delivery method label for Ocean eReferral", async () => {
    getPatientReferralsMock.mockResolvedValue([
      referral({ deliveryMethod: "OCEAN_EREFERRAL" }),
    ]);
    renderScreen();
    expect(await screen.findByText("Ocean eReferral")).toBeInTheDocument();
  });

  it("renders a 'View referral letter' link when documentUrl is set", async () => {
    getPatientReferralsMock.mockResolvedValue([
      referral({ documentUrl: "https://storage.example.com/ref-1.pdf", deliveryMethod: "PDF_FAX" }),
    ]);
    renderScreen();
    const link = await screen.findByRole("link", { name: /view referral letter/i });
    expect(link).toHaveAttribute("href", "https://storage.example.com/ref-1.pdf");
  });

  it("renders multiple referrals newest-first (order from API)", async () => {
    getPatientReferralsMock.mockResolvedValue([
      referral({ id: "ref-1", confirmedSpecialty: "Cardiology", createdAt: "2026-06-02T10:00:00Z" }),
      referral({ id: "ref-2", confirmedSpecialty: "Neurology", createdAt: "2026-06-01T10:00:00Z" }),
    ]);
    renderScreen();
    const items = await screen.findAllByText(/Cardiology|Neurology/);
    // API returns newest-first; first item should be Cardiology.
    expect(items[0].textContent).toContain("Cardiology");
    expect(items[1].textContent).toContain("Neurology");
  });
});
