import { api } from "@/lib/api";

import type { DashboardSummary } from "./types";

type Envelope<T> = { data: T };

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const res = await api.get<Envelope<DashboardSummary>>("/dashboard/summary");
  return res.data.data;
}
