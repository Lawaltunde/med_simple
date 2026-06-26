import { useState } from "react";
import { Loader2, UserPlus, BadgeCheck, Trash2, ShieldCheck } from "lucide-react";
import { ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { useMyClinics, useStaff, useAddStaff, useRemoveStaff, useVerifyStaff } from "@/features/clinic/hooks";
import { STAFF_ROLE_LABEL, VerificationBadge } from "@/features/clinic/display";
import type { AddStaffInput, StaffRole } from "@/features/clinic/api";

const EMPTY: AddStaffInput = {
  email: "", password: "", firstName: "", lastName: "", role: "PHYSICIAN",
  specialization: "", college: "", registrationNumber: "", licenseProvince: "",
};

export function StaffPage() {
  const { data: clinics } = useMyClinics();
  const clinicId = clinics?.[0]?.id;
  const { data: staff, isLoading } = useStaff(clinicId);
  const add = useAddStaff(clinicId ?? "");
  const remove = useRemoveStaff(clinicId ?? "");
  const verify = useVerifyStaff(clinicId ?? "");

  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<AddStaffInput>(EMPTY);
  const set = (k: keyof AddStaffInput, v: string) => setForm((f) => ({ ...f, [k]: v }));

  const addError = add.error instanceof ApiError ? add.error.message : add.error ? "Could not add staff." : null;
  const isPhysician = form.role === "PHYSICIAN";

  function submit() {
    if (!clinicId) return;
    add.mutate(form, { onSuccess: () => { setForm(EMPTY); setOpen(false); } });
  }

  return (
    <div className="mx-auto max-w-3xl">
      <header className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-2xl font-extrabold tracking-tight text-brand-900">Staff</h2>
          <p className="mt-1 text-muted">{clinics?.[0]?.name ?? "Your clinic"} · physicians, nurses, and reception</p>
        </div>
        <Button onClick={() => setOpen((o) => !o)} variant={open ? "outline" : "primary"}>
          <UserPlus className="size-4" /> {open ? "Cancel" : "Add staff"}
        </Button>
      </header>

      {open && (
        <div className="mb-6 rounded-2xl border border-line bg-white p-5">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="First name" value={form.firstName} onChange={(v) => set("firstName", v)} />
            <Field label="Last name" value={form.lastName} onChange={(v) => set("lastName", v)} />
            <Field label="Email" type="email" value={form.email} onChange={(v) => set("email", v)} />
            <Field label="Temporary password" type="password" value={form.password} onChange={(v) => set("password", v)} />
            <label className="block">
              <span className="mb-1.5 block text-sm font-medium text-ink">Role</span>
              <select
                value={form.role}
                onChange={(e) => set("role", e.target.value as StaffRole)}
                className="h-11 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink focus:border-brand-400 focus:outline-none"
              >
                {(["PHYSICIAN", "NURSE", "RECEPTIONIST", "CLINIC_ADMIN"] as StaffRole[]).map((r) => (
                  <option key={r} value={r}>{STAFF_ROLE_LABEL[r]}</option>
                ))}
              </select>
            </label>
            <Field label="Specialization (optional)" value={form.specialization ?? ""} onChange={(v) => set("specialization", v)} />
          </div>

          {isPhysician && (
            <div className="mt-3 grid gap-3 rounded-xl bg-brand-50/50 p-3 sm:grid-cols-3">
              <Field label="College (e.g. CPSO)" value={form.college ?? ""} onChange={(v) => set("college", v)} />
              <Field label="Registration #" value={form.registrationNumber ?? ""} onChange={(v) => set("registrationNumber", v)} />
              <Field label="Province" value={form.licenseProvince ?? ""} onChange={(v) => set("licenseProvince", v)} />
            </div>
          )}

          {addError && <p className="mt-3 text-sm text-[--color-urgency-emergency]" role="alert">{addError}</p>}

          <Button className="mt-4" onClick={submit} disabled={add.isPending || !clinicId}>
            {add.isPending ? <Loader2 className="size-4 animate-spin" /> : <UserPlus className="size-4" />} Add to clinic
          </Button>
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-muted">
          <Loader2 className="size-5 animate-spin" /> Loading…
        </div>
      ) : (
        <ul className="space-y-3">
          {(staff ?? []).map((s) => (
            <li key={s.id} className="flex items-center gap-4 rounded-2xl border border-line bg-white p-4">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink">{s.firstName} {s.lastName}</p>
                  <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-wide text-brand-700">
                    {STAFF_ROLE_LABEL[s.role]}
                  </span>
                  {s.role === "PHYSICIAN" && <VerificationBadge status={s.verificationStatus} />}
                </div>
                <p className="mt-0.5 truncate text-sm text-muted">{s.email}</p>
                {s.role === "PHYSICIAN" && (s.college || s.registrationNumber) && (
                  <p className="mt-0.5 text-xs text-muted">{s.college} {s.registrationNumber} {s.licenseProvince}</p>
                )}
              </div>
              {s.role === "PHYSICIAN" && s.verificationStatus !== "VERIFIED" && (
                <Button size="sm" variant="outline" onClick={() => verify.mutate(s.id)} disabled={verify.isPending}>
                  <ShieldCheck className="size-4" /> Verify
                </Button>
              )}
              {s.role === "PHYSICIAN" && s.verificationStatus === "VERIFIED" && (
                <BadgeCheck className="size-5 text-green-600" />
              )}
              <button
                type="button"
                onClick={() => remove.mutate(s.id)}
                disabled={remove.isPending}
                className="rounded-lg p-2 text-muted transition-colors hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                aria-label={`Remove ${s.firstName} ${s.lastName}`}
              >
                <Trash2 className="size-4" />
              </button>
            </li>
          ))}
          {staff && staff.length === 0 && (
            <li className="rounded-2xl border border-line bg-white px-5 py-10 text-center text-sm text-muted">
              No staff yet. Add your first physician to start taking patients.
            </li>
          )}
        </ul>
      )}
    </div>
  );
}

function Field({
  label, value, onChange, type = "text",
}: { label: string; value: string; onChange: (v: string) => void; type?: string }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-ink">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="h-11 w-full rounded-xl border border-line bg-white px-3 text-sm text-ink focus:border-brand-400 focus:outline-none"
      />
    </label>
  );
}
