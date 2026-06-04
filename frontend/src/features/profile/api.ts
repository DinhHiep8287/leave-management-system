import { api } from "@/lib/api";
import type { User } from "@/features/users/types";

type Envelope<T> = { data: T };

export async function getMyProfile(): Promise<User> {
  const res = await api.get<Envelope<User>>("/users/me");
  return res.data.data;
}

export async function updateMyName(fullName: string): Promise<User> {
  const res = await api.patch<Envelope<User>>("/users/me", { fullName });
  return res.data.data;
}

export async function changeMyPassword(oldPassword: string, newPassword: string): Promise<void> {
  await api.post("/users/me/password", { oldPassword, newPassword });
}
