import type { LeaveBalance } from "@/features/dashboard/types";
import { api } from "@/lib/api";

type Envelope<T> = { data: T };
type PageData<T> = { content: T[] };

export async function initializeYear(year: number): Promise<{ year: number; created: number }> {
  const res = await api.post<Envelope<{ year: number; created: number }>>(
    "/leave-balances/initialize",
    {},
    { params: { year } },
  );
  return res.data.data;
}

export async function carryOverYear(
  fromYear: number,
  capDays: number,
): Promise<{ fromYear: number; capDays: number; carried: number }> {
  const res = await api.post<Envelope<{ fromYear: number; capDays: number; carried: number }>>(
    "/leave-balances/carry-over",
    {},
    { params: { fromYear, capDays } },
  );
  return res.data.data;
}

export async function getUserBalances(userId: number, year: number): Promise<LeaveBalance[]> {
  const res = await api.get<Envelope<LeaveBalance[]>>(`/users/${userId}/leave-balances`, {
    params: { year },
  });
  return res.data.data;
}

export async function adjustBalance(
  id: number,
  adjustedDaysDelta: number,
  reason: string,
): Promise<LeaveBalance> {
  const res = await api.patch<Envelope<LeaveBalance>>(`/leave-balances/${id}/adjust`, {
    adjustedDaysDelta,
    reason,
  });
  return res.data.data;
}

export type UserOption = { id: number; fullName: string; employeeCode: string; departmentId: number | null };

export async function listUserOptions(): Promise<UserOption[]> {
  const res = await api.get<Envelope<PageData<UserOption>>>("/users", {
    params: { activeOnly: true, size: 200 },
  });
  return res.data.data.content.map((u) => ({
    id: u.id,
    fullName: u.fullName,
    employeeCode: u.employeeCode,
    departmentId: u.departmentId,
  }));
}
