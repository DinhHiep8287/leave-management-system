import type { LeaveStatus } from "@/features/leave-requests/types";
import { api } from "@/lib/api";

export async function downloadLeaveRequestsCsv(
  from: string,
  to: string,
  status?: LeaveStatus,
): Promise<Blob> {
  const res = await api.get("/reports/leave-requests.csv", {
    params: { from, to, status },
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
