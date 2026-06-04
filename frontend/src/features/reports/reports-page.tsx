import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { STATUS_LABELS, type LeaveStatus } from "@/features/leave-requests/types";
import { useDepartmentOptions } from "@/features/users/hooks";
import { saveBlob } from "@/lib/download";

import { downloadLeaveBalancesCsv, downloadLeaveRequestsCsv, downloadLeaveSummaryCsv } from "./api";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = [CURRENT_YEAR, CURRENT_YEAR - 1];
const STATUSES: LeaveStatus[] = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"];

export function ReportsPage() {
  const [from, setFrom] = useState(`${CURRENT_YEAR}-01-01`);
  const [to, setTo] = useState(`${CURRENT_YEAR}-12-31`);
  const [status, setStatus] = useState<LeaveStatus | undefined>(undefined);
  const [departmentId, setDepartmentId] = useState<number | undefined>(undefined);
  const [year, setYear] = useState(CURRENT_YEAR);
  const [summaryYear, setSummaryYear] = useState(CURRENT_YEAR);
  const [groupBy, setGroupBy] = useState<"month" | "quarter">("month");

  const { data: depts } = useDepartmentOptions();

  const requestsCsv = useMutation({
    mutationFn: () => downloadLeaveRequestsCsv(from, to, status, departmentId),
    onSuccess: (blob) => saveBlob(blob, `leave-requests_${from}_${to}.csv`),
    onError: () => toast.error("Tải báo cáo thất bại"),
  });

  const balancesCsv = useMutation({
    mutationFn: () => downloadLeaveBalancesCsv(year),
    onSuccess: (blob) => saveBlob(blob, `leave-balances_${year}.csv`),
    onError: () => toast.error("Tải báo cáo thất bại"),
  });

  const summaryCsv = useMutation({
    mutationFn: () => downloadLeaveSummaryCsv(summaryYear, groupBy),
    onSuccess: (blob) => saveBlob(blob, `leave-summary_${summaryYear}_${groupBy}.csv`),
    onError: () => toast.error("Tải báo cáo thất bại"),
  });

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Báo cáo</h1>
        <p className="text-sm text-muted-foreground">Xuất dữ liệu ra tệp CSV (mở được bằng Excel).</p>
      </header>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Đơn nghỉ phép</CardTitle>
            <CardDescription>Các đơn có khoảng thời gian giao với kỳ đã chọn.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="from">Từ ngày</Label>
                <Input id="from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="to">Đến ngày</Label>
                <Input id="to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="status">Trạng thái</Label>
              <Select
                id="status"
                value={status ?? ""}
                onChange={(e) => setStatus((e.target.value || undefined) as LeaveStatus | undefined)}
              >
                <option value="">Mọi trạng thái</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_LABELS[s]}
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="dept">Phòng ban</Label>
              <Select
                id="dept"
                value={departmentId ?? ""}
                onChange={(e) => setDepartmentId(e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">Mọi phòng ban</option>
                {depts?.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </Select>
            </div>
            <Button disabled={requestsCsv.isPending} onClick={() => requestsCsv.mutate()}>
              {requestsCsv.isPending ? "Đang tạo…" : "Tải CSV đơn nghỉ"}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Quỹ phép</CardTitle>
            <CardDescription>Quỹ phép của toàn bộ nhân viên trong năm.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="year">Năm</Label>
              <Select id="year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
                {YEARS.map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
              </Select>
            </div>
            <Button disabled={balancesCsv.isPending} onClick={() => balancesCsv.mutate()}>
              {balancesCsv.isPending ? "Đang tạo…" : "Tải CSV quỹ phép"}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Tổng hợp theo loại</CardTitle>
            <CardDescription>Tổng số ngày đã duyệt theo loại nghỉ, theo tháng hoặc quý.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="summary-year">Năm</Label>
                <Select
                  id="summary-year"
                  value={summaryYear}
                  onChange={(e) => setSummaryYear(Number(e.target.value))}
                >
                  {YEARS.map((y) => (
                    <option key={y} value={y}>
                      {y}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="groupBy">Nhóm theo</Label>
                <Select
                  id="groupBy"
                  value={groupBy}
                  onChange={(e) => setGroupBy(e.target.value as "month" | "quarter")}
                >
                  <option value="month">Tháng</option>
                  <option value="quarter">Quý</option>
                </Select>
              </div>
            </div>
            <Button disabled={summaryCsv.isPending} onClick={() => summaryCsv.mutate()}>
              {summaryCsv.isPending ? "Đang tạo…" : "Tải CSV tổng hợp"}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
