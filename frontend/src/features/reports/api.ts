import type { LeaveStatus } from "@/features/leave-requests/types";
import { api } from "@/lib/api";

export async function downloadLeaveRequestsCsv(
  from: string,
  to: string,
  status?: LeaveStatus,
  departmentId?: number,
): Promise<Blob> {
  const res = await api.get("/reports/leave-requests.csv", {
    params: { from, to, status, departmentId },
    responseType: "blob",
  });
  return res.data as Blob;
}

export async function downloadLeaveSummaryCsv(year: number, groupBy: "month" | "quarter"): Promise<Blob> {
  const res = await api.get("/reports/leave-summary.csv", {
    params: { year, groupBy },
    responseType: "blob",
  });
  return res.data as Blob;
}

export async function downloadLeaveBalancesCsv(year: number): Promise<Blob> {
  const res = await api.get("/reports/leave-balances.csv", {
    params: { year },
    responseType: "blob",
  });
  return res.data as Blob;
}
