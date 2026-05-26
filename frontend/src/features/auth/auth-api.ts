import { api } from "@/lib/api";

import type { Me, TokenPair } from "./types";

type Envelope<T> = { data: T };

export async function loginRequest(email: string, password: string): Promise<TokenPair> {
  const res = await api.post<Envelope<TokenPair>>("/auth/login", { email, password });
  return res.data.data;
}

export async function refreshRequest(refreshToken: string): Promise<TokenPair> {
  const res = await api.post<Envelope<TokenPair>>("/auth/refresh", { refreshToken });
  return res.data.data;
}

export async function logoutRequest(refreshToken: string): Promise<void> {
  await api.post("/auth/logout", { refreshToken });
}

export async function getMeRequest(): Promise<Me> {
  const res = await api.get<Envelope<Me>>("/auth/me");
  return res.data.data;
}
