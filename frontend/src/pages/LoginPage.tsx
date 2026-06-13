import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Eye, EyeOff, Loader2 } from "lucide-react";
import { AuthLayout } from "@/components/AuthLayout";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/AuthProvider";
import { homePathForRole } from "@/auth/types";
import { ApiError } from "@/lib/api";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    const form = new FormData(e.currentTarget);
    try {
      const user = await login(String(form.get("email")), String(form.get("password")));
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from ?? homePathForRole(user.role), { replace: true });
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? "Incorrect email or password." : "Something went wrong. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <h1 className="text-3xl font-extrabold tracking-tight text-brand-900">Welcome back</h1>
      <p className="mt-2 text-muted">Log in to continue your care.</p>

      <form className="mt-8 space-y-5" onSubmit={onSubmit}>
        <Input name="email" type="email" label="Email address" placeholder="you@example.com" required autoComplete="email" />

        <div className="relative">
          <Input
            name="password"
            type={showPassword ? "text" : "password"}
            label="Password"
            placeholder="Enter your password"
            required
            autoComplete="current-password"
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
          {submitting ? <Loader2 className="size-5 animate-spin" /> : "Log in"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        New to MedSimple?{" "}
        <Link to="/signup" className="font-semibold text-brand-600 hover:underline">
          Create an account
        </Link>
      </p>
    </AuthLayout>
  );
}
