import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import axios from "axios";
import { fetchCurrentEmployee, login as loginRequest, logout as logoutRequest } from "@/api/auth";
import { clearTokens, getRefreshToken, setAccessToken, setRefreshToken } from "@/api/tokenStore";
import type { EmployeeResponse } from "@/types";

interface AuthContextValue {
  employee: EmployeeResponse | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [employee, setEmployee] = useState<EmployeeResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Access tokens live only in memory (see api/tokenStore.ts), so a page reload always needs
    // a silent refresh (using the persisted refresh token) before we know who's logged in.
    const existingRefreshToken = getRefreshToken();
    if (!existingRefreshToken) {
      setIsLoading(false);
      return;
    }

    axios
      .post(`${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1"}/auth/refresh`, {
        refreshToken: existingRefreshToken,
      })
      .then(async (response) => {
        setAccessToken(response.data.accessToken);
        setRefreshToken(response.data.refreshToken);
        const currentEmployee = await fetchCurrentEmployee();
        setEmployee(currentEmployee);
      })
      .catch(() => {
        clearTokens();
      })
      .finally(() => setIsLoading(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await loginRequest(email, password);
    setAccessToken(tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
    const currentEmployee = await fetchCurrentEmployee();
    setEmployee(currentEmployee);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) {
        await logoutRequest(refreshToken);
      }
    } finally {
      clearTokens();
      setEmployee(null);
    }
  }, []);

  const value = useMemo(() => ({ employee, isLoading, login, logout }), [employee, isLoading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
