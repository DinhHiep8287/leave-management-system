import { api } from "@/lib/api";

import type { MyProfile } from "./types";

type Envelope<T> = { data: T };

export async function getMyProfile(): Promise<MyProfile> {
  const res = await api.get<Envelope<MyProfile>>("/users/me");
  return res.data.data;
}

export async function updateMyName(fullName: string): Promise<MyProfile> {
  const res = await api.patch<Envelope<MyProfile>>("/users/me", { fullName });
  return res.data.data;
}

export async function changeMyPassword(oldPassword: string, newPassword: string): Promise<void> {
  await api.post("/users/me/password", { oldPassword, newPassword });
}
