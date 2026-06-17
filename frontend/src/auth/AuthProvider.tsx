import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from "react";
import { api, tokenStore, registerRefreshHandler } from "@/lib/api";
import type { AuthTokens, CurrentUser } from "./types";

// The refresh token lives in an httpOnly cookie set by the backend, so JS never touches
// it (XSS can't exfiltrate it). The browser sends it automatically to /api/v1/auth on
// refresh and logout. Only the short-lived access token is held here, in memory.

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

  const applyTokens = useCallback((auth: AuthTokens) => {
    tokenStore.set(auth.accessToken);
  }, []);

  const clearSession = useCallback(() => {
    tokenStore.set(null);
    setUser(null);
  }, []);

  // Exchange the httpOnly refresh cookie for a fresh access token. The cookie rides
  // along automatically; no token is read or sent by JS. Returns the new access token
  // (for the api layer's silent retry) or null if there is no live session.
  const refresh = useCallback(async (): Promise<string | null> => {
    try {
      const auth = await api<AuthTokens>("/auth/refresh", { method: "POST" }, { auth: false });
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
      const auth = await api<AuthTokens>(
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
    // Server revokes the refresh token and clears the cookie; JS just drops the access token.
    await api("/auth/logout", { method: "POST" }, { auth: false }).catch(() => {});
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
