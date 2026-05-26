import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";

import { tokenStorage } from "@/features/auth/token-storage";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
  withCredentials: false,
  timeout: 10_000,
});

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

const PUBLIC_AUTH_PATHS = ["/auth/login", "/auth/refresh", "/auth/logout"];

api.interceptors.request.use((config) => {
  const isPublic = PUBLIC_AUTH_PATHS.some((p) => config.url?.endsWith(p));
  if (!isPublic) {
    const access = tokenStorage.getAccess();
    if (access) config.headers.set("Authorization", `Bearer ${access}`);
  }
  return config;
});

// Single-flight refresh: collapse concurrent 401s into one refresh call
let refreshInFlight: Promise<string | null> | null = null;
let onAuthFailure: (() => void) | null = null;

export function setAuthFailureHandler(handler: (() => void) | null): void {
  onAuthFailure = handler;
}

async function performRefresh(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefresh();
  if (!refreshToken) return null;
  try {
    const res = await api.post<{ data: { accessToken: string; refreshToken: string } }>(
      "/auth/refresh",
      { refreshToken },
    );
    tokenStorage.setAccess(res.data.data.accessToken);
    tokenStorage.setRefresh(res.data.data.refreshToken);
    return res.data.data.accessToken;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryableConfig | undefined;
    if (!original || original._retry) return Promise.reject(error);
    if (error.response?.status !== 401) return Promise.reject(error);
    if (original.url && PUBLIC_AUTH_PATHS.some((p) => original.url?.endsWith(p))) {
      return Promise.reject(error);
    }

    original._retry = true;
    refreshInFlight ??= performRefresh().finally(() => {
      refreshInFlight = null;
    });
    const newAccess = await refreshInFlight;
    if (!newAccess) {
      onAuthFailure?.();
      return Promise.reject(error);
    }
    original.headers.set("Authorization", `Bearer ${newAccess}`);
    return api(original);
  },
);
