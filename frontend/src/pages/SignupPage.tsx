import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Eye, EyeOff, Loader2 } from "lucide-react";
import { AuthLayout } from "@/components/AuthLayout";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/AuthProvider";
import { listClinics } from "@/features/clinic/api";
import { ApiError } from "@/lib/api";

const NO_CLINIC = "";

export function SignupPage() {
  const { signupPatient } = useAuth();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { data: clinics } = useQuery({ queryKey: ["clinics"], queryFn: listClinics });

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    const password = String(form.get("password"));
    if (password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    setSubmitting(true);
    try {
      const clinicId = String(form.get("clinicId"));
      await signupPatient({
        firstName: String(form.get("firstName")),
        lastName: String(form.get("lastName")),
        email: String(form.get("email")),
        password,
        phone: String(form.get("phone")) || undefined,
        clinicId: clinicId === NO_CLINIC ? undefined : clinicId,
      });
      navigate("/app", { replace: true });
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? "An account with this email already exists."
          : "Something went wrong. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <h1 className="text-3xl font-extrabold tracking-tight text-brand-900">Create your account</h1>
      <p className="mt-2 text-muted">It's free, and a physician reviews every answer.</p>

      <form className="mt-8 space-y-5" onSubmit={onSubmit}>
        <div className="grid grid-cols-2 gap-4">
          <Input name="firstName" label="First name" placeholder="Jane" required autoComplete="given-name" />
          <Input name="lastName" label="Last name" placeholder="Doe" required autoComplete="family-name" />
        </div>
        <Input name="email" type="email" label="Email address" placeholder="you@example.com" required autoComplete="email" />
        <Input name="phone" type="tel" label="Phone (optional)" placeholder="416-555-0100" autoComplete="tel" />

        <div className="w-full">
          <label htmlFor="clinicId" className="mb-1.5 block text-sm font-medium text-ink">
            Your clinic (optional)
          </label>
          <select
            id="clinicId"
            name="clinicId"
            defaultValue={NO_CLINIC}
            className="h-12 w-full rounded-xl border border-line bg-white px-4 text-[0.95rem] text-ink shadow-sm transition-colors focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25"
          >
            <option value={NO_CLINIC}>I don't have a family doctor yet</option>
            {clinics?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
                {c.city ? ` — ${c.city}` : ""}
              </option>
            ))}
          </select>
          <p className="mt-1.5 text-sm text-muted">
            Complaints route to your clinic. No clinic? You can still send to a walk-in later.
          </p>
        </div>

        <div className="relative">
          <Input
            name="password"
            type={showPassword ? "text" : "password"}
            label="Password"
            placeholder="At least 8 characters"
            required
            autoComplete="new-password"
            className="pr-12"
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-3 top-[2.55rem] rounded-md p-1 text-muted hover:text-ink"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
          </button>
        </div>

        {error && (
          <div role="alert" className="rounded-xl bg-[--color-urgency-emergency]/8 px-4 py-3 text-sm text-[--color-urgency-emergency]">
            {error}
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={submitting}>
          {submitting ? <Loader2 className="size-5 animate-spin" /> : "Create account"}
        </Button>

        <p className="text-center text-xs leading-relaxed text-muted">
          By creating an account you agree that MedSimple is not for emergencies. If this is an
          emergency, call 911.
        </p>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        Already have an account?{" "}
        <Link to="/login" className="font-semibold text-brand-600 hover:underline">
          Log in
        </Link>
      </p>
    </AuthLayout>
  );
}
