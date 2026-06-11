import { useState } from "react";

import { ErrorState } from "@/components/error-state";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { TableSkeletonRows } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { RequestDetailDialog } from "@/features/leave-requests/request-detail-dialog";
import { STATUS_LABELS, type LeaveStatus } from "@/features/leave-requests/types";
import { formatDate } from "@/lib/format";

import { DecisionDialog, type DecisionMode } from "./decision-dialog";
import { useInbox } from "./hooks";

const STATUSES: LeaveStatus[] = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"];

export function ApprovalInboxPage() {
  const [status, setStatus] = useState<LeaveStatus | undefined>("PENDING");
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [decision, setDecision] = useState<{ mode: DecisionMode; id: number } | null>(null);

  const { data, isLoading, isFetching, isError, refetch } = useInbox(status, page);
  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 1;

  const changeStatus = (next: LeaveStatus | undefined) => {
    setStatus(next);
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Cần duyệt</h1>
        <p className="text-sm text-muted-foreground">
          Đơn nghỉ phép của nhân viên thuộc phạm vi duyệt của bạn.
        </p>
      </header>

      {isError && <ErrorState onRetry={() => void refetch()} />}

      <div className="flex flex-wrap items-center gap-3">
        <Select
          aria-label="Trạng thái"
          className="w-44"
          value={status ?? ""}
          onChange={(e) => changeStatus((e.target.value || undefined) as LeaveStatus | undefined)}
        >
          <option value="">Mọi trạng thái</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </Select>
        {isFetching && <span className="text-xs text-muted-foreground">Đang tải…</span>}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nhân viên</TableHead>
              <TableHead>Loại</TableHead>
              <TableHead>Từ ngày</TableHead>
              <TableHead>Đến ngày</TableHead>
              <TableHead>Số ngày</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && <TableSkeletonRows rows={4} colSpan={7} />}
            {!isLoading && items.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="py-8 text-center text-muted-foreground">
                  Không có đơn nào trong bộ lọc này — bạn đã xử lý hết.
                </TableCell>
              </TableRow>
            )}
            {items.map((r) => (
              <TableRow key={r.id}>
                <TableCell className="font-medium">{r.userFullName}</TableCell>
                <TableCell>{r.leaveTypeCode}</TableCell>
                <TableCell>{formatDate(r.startDate)}</TableCell>
                <TableCell>{formatDate(r.endDate)}</TableCell>
                <TableCell>{r.totalDays}</TableCell>
                <TableCell>
                  <StatusBadge status={r.status} />
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button variant="ghost" size="sm" onClick={() => setDetailId(r.id)}>
                      Xem
                    </Button>
                    {r.status === "PENDING" && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => setDecision({ mode: "approve", id: r.id })}
                        >
                          Duyệt
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setDecision({ mode: "reject", id: r.id })}
                        >
                          Từ chối
                        </Button>
                      </>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-3 text-sm">
          <span className="text-muted-foreground">
            Trang {page + 1} / {totalPages} · {data?.totalElements ?? 0} đơn
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Trước
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </Button>
        </div>
      )}

      <RequestDetailDialog requestId={detailId} onClose={() => setDetailId(null)} />
      <DecisionDialog
        mode={decision?.mode ?? null}
        requestId={decision?.id ?? null}
        onClose={() => setDecision(null)}
      />
    </div>
  );
}
