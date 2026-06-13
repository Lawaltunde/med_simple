import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from "react";
import { api, tokenStore, registerRefreshHandler } from "@/lib/api";
import type { AuthResponse, CurrentUser } from "./types";

// Refresh token persists across reloads so a session survives a page refresh.
// (Access token stays in memory only.) httpOnly cookies would be stronger; the
// backend returns tokens in JSON, so this is the pragmatic SPA approach.
const REFRESH_KEY = "medsimple.refreshToken";

interface AuthContextValue {
  user: CurrentUser | null;
  initializing: boolean;
  login: (email: string, password: string) => Promise<CurrentUser>;
  signupPatient: (input: SignupInput) => Promise<CurrentUser>;
  logout: () => Promise<void>;
}

export interface SignupInput {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [initializing, setInitializing] = useState(true);

  const applyTokens = useCallback((auth: AuthResponse) => {
    tokenStore.set(auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
  }, []);

  const clearSession = useCallback(() => {
    tokenStore.set(null);
    localStorage.removeItem(REFRESH_KEY);
    setUser(null);
  }, []);

  // Exchange the stored refresh token for a fresh access token. Returns the new
  // access token (for the api layer's silent retry) or null if the session is dead.
  const refresh = useCallback(async (): Promise<string | null> => {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (!refreshToken) return null;
    try {
      const auth = await api<AuthResponse>(
        "/auth/refresh",
        { method: "POST", body: JSON.stringify({ refreshToken }) },
        { auth: false }
      );
      applyTokens(auth);
      return auth.accessToken;
    } catch {
      clearSession();
      return null;
    }
  }, [applyTokens, clearSession]);

  const loadCurrentUser = useCallback(async () => {
    const me = await api<CurrentUser>("/auth/me");
    setUser(me);
    return me;
  }, []);

  // Wire the api layer's silent-refresh to this provider.
  useEffect(() => {
    registerRefreshHandler(refresh);
    return () => registerRefreshHandler(null);
  }, [refresh]);

  // On first load, try to restore a session from the persisted refresh token.
  useEffect(() => {
    (async () => {
      const fresh = await refresh();
      if (fresh) {
        try {
          await loadCurrentUser();
        } catch {
          clearSession();
        }
      }
      setInitializing(false);
    })();
  }, [refresh, loadCurrentUser, clearSession]);

  const login = useCallback(
    async (email: string, password: string) => {
      const auth = await api<AuthResponse>(
        "/auth/login",
        { method: "POST", body: JSON.stringify({ email, password }) },
        { auth: false }
      );
      applyTokens(auth);
      return loadCurrentUser();
    },
    [applyTokens, loadCurrentUser]
  );

  const signupPatient = useCallback(
    async (input: SignupInput) => {
      // Public self-registration creates the patient, then we log them straight in.
      await api("/patients", { method: "POST", body: JSON.stringify(input) }, { auth: false });
      return login(input.email, input.password);
    },
    [login]
  );

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      await api("/auth/logout", { method: "POST", body: JSON.stringify({ refreshToken }) }, { auth: false }).catch(
        () => {}
      );
    }
    clearSession();
  }, [clearSession]);

  return (
    <AuthContext.Provider value={{ user, initializing, login, signupPatient, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within <AuthProvider>");
  return ctx;
}
