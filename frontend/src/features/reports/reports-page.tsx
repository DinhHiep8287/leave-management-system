import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { STATUS_LABELS, type LeaveStatus } from "@/features/leave-requests/types";
import { useDepartmentOptions } from "@/features/users/hooks";
import { saveBlob } from "@/lib/download";

import {
  downloadLeaveBalancesCsv,
  downloadLeaveRequestsCsv,
  downloadLeaveSummaryCsv,
  getLeaveSummary,
} from "./api";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = [CURRENT_YEAR + 1, CURRENT_YEAR, CURRENT_YEAR - 1];
const STATUSES: LeaveStatus[] = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"];

export function ReportsPage() {
  const [from, setFrom] = useState(`${CURRENT_YEAR}-01-01`);
  const [to, setTo] = useState(`${CURRENT_YEAR}-12-31`);
  const [status, setStatus] = useState<LeaveStatus | undefined>(undefined);
  const [requestDepartmentId, setRequestDepartmentId] = useState<number | undefined>(undefined);
  const [balanceDepartmentId, setBalanceDepartmentId] = useState<number | undefined>(undefined);
  const [summaryDepartmentId, setSummaryDepartmentId] = useState<number | undefined>(undefined);
  const [year, setYear] = useState(CURRENT_YEAR);
  const [summaryYear, setSummaryYear] = useState(CURRENT_YEAR);
  const [groupBy, setGroupBy] = useState<"month" | "quarter">("month");

  const { data: depts } = useDepartmentOptions();
  const { data: summaryRows, isLoading: summaryLoading } = useQuery({
    queryKey: ["report-summary", summaryYear, groupBy, summaryDepartmentId ?? "all"],
    queryFn: () => getLeaveSummary(summaryYear, groupBy, summaryDepartmentId),
  });

  const summaryTotals = useMemo(() => {
    const rows = summaryRows ?? [];
    return {
      totalDays: rows.reduce((sum, row) => sum + Number(row.totalDays), 0),
      requestCount: rows.reduce((sum, row) => sum + row.requestCount, 0),
      leaveTypeCount: new Set(rows.map((row) => row.leaveTypeCode)).size,
    };
  }, [summaryRows]);

  const requestsCsv = useMutation({
    mutationFn: () => downloadLeaveRequestsCsv(from, to, status, requestDepartmentId),
    onSuccess: (blob) => saveBlob(blob, `leave-requests_${from}_${to}.csv`),
    onError: () => toast.error("Tải báo cáo thất bại"),
  });

  const balancesCsv = useMutation({
    mutationFn: () => downloadLeaveBalancesCsv(year, balanceDepartmentId),
    onSuccess: (blob) => saveBlob(blob, `leave-balances_${year}.csv`),
    onError: () => toast.error("Tải báo cáo thất bại"),
  });

  const summaryCsv = useMutation({
    mutationFn: () => downloadLeaveSummaryCsv(summaryYear, groupBy, summaryDepartmentId),
    onSuccess: (blob) => saveBlob(blob, `leave-summary_${summaryYear}_${groupBy}.csv`),
    onError: () => toast.error("Tải báo cáo thất bại"),
  });

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Báo cáo</h1>
        <p className="text-sm text-muted-foreground">
          Xuất dữ liệu CSV và xem nhanh số liệu tổng hợp theo phòng ban, loại nghỉ, tháng hoặc quý.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Tổng hợp ngày nghỉ đã duyệt</CardTitle>
          <CardDescription>
            Bảng preview giúp kiểm tra số liệu trước khi tải CSV, có thể lọc theo phòng ban.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-3">
            <YearSelect id="summary-year" label="Năm" value={summaryYear} onChange={setSummaryYear} />
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
            <DepartmentSelect
              id="summary-dept"
              label="Phòng ban"
              value={summaryDepartmentId}
              departments={depts}
              onChange={setSummaryDepartmentId}
            />
          </div>

          <div className="grid gap-3 md:grid-cols-3">
            <MetricCard label="Tổng ngày nghỉ" value={formatNumber(summaryTotals.totalDays)} />
            <MetricCard label="Số đơn đã duyệt" value={String(summaryTotals.requestCount)} />
            <MetricCard label="Loại nghỉ có phát sinh" value={String(summaryTotals.leaveTypeCount)} />
          </div>

          <div className="rounded-lg border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{groupBy === "quarter" ? "Quý" : "Tháng"}</TableHead>
                  <TableHead>Loại nghỉ</TableHead>
                  <TableHead className="text-right">Tổng ngày</TableHead>
                  <TableHead className="text-right">Số đơn</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {summaryLoading && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      Đang tải số liệu...
                    </TableCell>
                  </TableRow>
                )}
                {!summaryLoading && (summaryRows?.length ?? 0) === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      Không có dữ liệu phù hợp với bộ lọc.
                    </TableCell>
                  </TableRow>
                )}
                {summaryRows?.map((row) => (
                  <TableRow key={`${row.period}-${row.leaveTypeCode}`}>
                    <TableCell className="font-medium">{row.period}</TableCell>
                    <TableCell>{row.leaveTypeCode}</TableCell>
                    <TableCell className="text-right">{formatNumber(row.totalDays)}</TableCell>
                    <TableCell className="text-right">{row.requestCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <Button disabled={summaryCsv.isPending} onClick={() => summaryCsv.mutate()}>
            {summaryCsv.isPending ? "Đang tạo..." : "Tải CSV tổng hợp"}
          </Button>
        </CardContent>
      </Card>

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
            <div className="grid gap-3 sm:grid-cols-2">
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
              <DepartmentSelect
                id="request-dept"
                label="Phòng ban"
                value={requestDepartmentId}
                departments={depts}
                onChange={setRequestDepartmentId}
              />
            </div>
            <Button disabled={requestsCsv.isPending} onClick={() => requestsCsv.mutate()}>
              {requestsCsv.isPending ? "Đang tạo..." : "Tải CSV đơn nghỉ"}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Quỹ phép</CardTitle>
            <CardDescription>Quỹ phép của nhân viên trong năm, có thể lọc theo phòng ban.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <YearSelect id="year" label="Năm" value={year} onChange={setYear} />
              <DepartmentSelect
                id="balance-dept"
                label="Phòng ban"
                value={balanceDepartmentId}
                departments={depts}
                onChange={setBalanceDepartmentId}
              />
            </div>
            <Button disabled={balancesCsv.isPending} onClick={() => balancesCsv.mutate()}>
              {balancesCsv.isPending ? "Đang tạo..." : "Tải CSV quỹ phép"}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border bg-secondary/40 p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-xl font-semibold">{value}</p>
    </div>
  );
}

function YearSelect({
  id,
  label,
  value,
  onChange,
}: {
  id: string;
  label: string;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>{label}</Label>
      <Select id={id} value={value} onChange={(e) => onChange(Number(e.target.value))}>
        {YEARS.map((y) => (
          <option key={y} value={y}>
            {y}
          </option>
        ))}
      </Select>
    </div>
  );
}

function DepartmentSelect({
  id,
  label,
  value,
  departments,
  onChange,
}: {
  id: string;
  label: string;
  value: number | undefined;
  departments: Array<{ id: number; name: string }> | undefined;
  onChange: (value: number | undefined) => void;
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>{label}</Label>
      <Select id={id} value={value ?? ""} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}>
        <option value="">Mọi phòng ban</option>
        {departments?.map((d) => (
          <option key={d.id} value={d.id}>
            {d.name}
          </option>
        ))}
      </Select>
    </div>
  );
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(value);
}
