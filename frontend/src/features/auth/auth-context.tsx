import { useQueryClient } from "@tanstack/react-query";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { setAuthFailureHandler } from "@/lib/api";

import { getMeRequest, loginRequest, logoutRequest } from "./auth-api";
import { tokenStorage } from "./token-storage";
import type { Me } from "./types";

type AuthStatus = "loading" | "authenticated" | "anonymous";

type AuthContextValue = {
  status: AuthStatus;
  user: Me | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<Me | null>(null);
  const queryClient = useQueryClient();

  const clearSession = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
    setStatus("anonymous");
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    setAuthFailureHandler(clearSession);
    return () => setAuthFailureHandler(null);
  }, [clearSession]);

  // Bootstrap: if a refresh token exists, axios interceptor will refresh
  // transparently when /auth/me hits a 401 (no access token yet).
  useEffect(() => {
    const refresh = tokenStorage.getRefresh();
    if (!refresh) {
      setStatus("anonymous");
      return;
    }
    getMeRequest()
      .then((me) => {
        setUser(me);
        setStatus("authenticated");
      })
      .catch(() => {
        clearSession();
      });
  }, [clearSession]);

  const login = useCallback(
    async (email: string, password: string) => {
      const pair = await loginRequest(email, password);
      tokenStorage.setAccess(pair.accessToken);
      tokenStorage.setRefresh(pair.refreshToken);
      const me = await getMeRequest();
      setUser(me);
      setStatus("authenticated");
    },
    [],
  );

  const logout = useCallback(async () => {
    const refresh = tokenStorage.getRefresh();
    if (refresh) {
      try {
        await logoutRequest(refresh);
      } catch {
        // best effort
      }
    }
    clearSession();
  }, [clearSession]);

  const value = useMemo(() => ({ status, user, login, logout }), [status, user, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
