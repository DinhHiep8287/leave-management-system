import { api } from "@/lib/api";

import type { AdminSummary, DashboardSummary } from "./types";

type Envelope<T> = { data: T };

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const res = await api.get<Envelope<DashboardSummary>>("/dashboard/summary");
  return res.data.data;
}

export async function getAdminSummary(): Promise<AdminSummary> {
  const res = await api.get<Envelope<AdminSummary>>("/dashboard/admin-summary");
  return res.data.data;
}
