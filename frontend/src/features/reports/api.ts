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

export type LeaveSummaryRow = {
  period: string;
  leaveTypeCode: string;
  totalDays: number;
  requestCount: number;
};

type Envelope<T> = { data: T };

export async function getLeaveSummary(
  year: number,
  groupBy: "month" | "quarter",
  departmentId?: number,
): Promise<LeaveSummaryRow[]> {
  const res = await api.get<Envelope<LeaveSummaryRow[]>>("/reports/leave-summary", {
    params: { year, groupBy, departmentId },
  });
  return res.data.data;
}

export async function downloadLeaveSummaryCsv(
  year: number,
  groupBy: "month" | "quarter",
  departmentId?: number,
): Promise<Blob> {
  const res = await api.get("/reports/leave-summary.csv", {
    params: { year, groupBy, departmentId },
    responseType: "blob",
  });
  return res.data as Blob;
}

export async function downloadLeaveBalancesCsv(year: number, departmentId?: number): Promise<Blob> {
  const res = await api.get("/reports/leave-balances.csv", {
    params: { year, departmentId },
    responseType: "blob",
  });
  return res.data as Blob;
}
