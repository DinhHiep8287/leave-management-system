import { useState } from "react";
import { Link } from "react-router-dom";

import { ErrorState } from "@/components/error-state";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Select } from "@/components/ui/select";
import { TableSkeletonRows } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAuth } from "@/features/auth/auth-context";
import { formatDate } from "@/lib/format";

import { EditRequestDialog } from "./edit-dialog";
import { useCancelRequest, useMyRequests } from "./hooks";
import { RequestDetailDialog } from "./request-detail-dialog";
import { STATUS_LABELS, type LeaveRequestResponse, type LeaveStatus } from "./types";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = [CURRENT_YEAR, CURRENT_YEAR - 1];
const STATUSES: LeaveStatus[] = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"];

// The requester may cancel a PENDING request, or an APPROVED one before it starts (§5.5).
function canRequesterCancel(status: LeaveStatus, startDate: string, todayIso: string): boolean {
  return status === "PENDING" || (status === "APPROVED" && startDate > todayIso);
}

export function MyRequestsPage() {
  const { user } = useAuth();
  const [year, setYear] = useState<number | undefined>(CURRENT_YEAR);
  const [status, setStatus] = useState<LeaveStatus | undefined>(undefined);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [cancelId, setCancelId] = useState<number | null>(null);
  const [editRequest, setEditRequest] = useState<LeaveRequestResponse | null>(null);

  const { data: requests, isLoading, isError, refetch } = useMyRequests(user?.id, year, status);
  const cancel = useCancelRequest();
  const todayIso = new Date().toISOString().slice(0, 10);

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Đơn của tôi</h1>
          <p className="text-sm text-muted-foreground">Theo dõi và quản lý đơn nghỉ phép của bạn.</p>
        </div>
        <Button asChild>
          <Link to="/leave-requests/new">Nộp đơn mới</Link>
        </Button>
      </header>

      {isError && <ErrorState onRetry={() => void refetch()} />}

      <div className="flex flex-wrap gap-3">
        <Select
          aria-label="Năm"
          className="w-40"
          value={year ?? ""}
          onChange={(e) => setYear(e.target.value ? Number(e.target.value) : undefined)}
        >
          <option value="">Tất cả các năm</option>
          {YEARS.map((y) => (
            <option key={y} value={y}>
              Năm {y}
            </option>
          ))}
        </Select>
        <Select
          aria-label="Trạng thái"
          className="w-44"
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

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Loại</TableHead>
              <TableHead>Từ ngày</TableHead>
              <TableHead>Đến ngày</TableHead>
              <TableHead>Số ngày</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && <TableSkeletonRows rows={4} colSpan={6} />}
            {!isLoading && requests?.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                  <p>Chưa có đơn nào trong bộ lọc này.</p>
                  <Button asChild variant="outline" size="sm" className="mt-3">
                    <Link to="/leave-requests/new">Nộp đơn mới</Link>
                  </Button>
                </TableCell>
              </TableRow>
            )}
            {requests?.map((r) => (
              <TableRow key={r.id}>
                <TableCell className="font-medium">{r.leaveTypeCode}</TableCell>
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
                      <Button variant="ghost" size="sm" onClick={() => setEditRequest(r)}>
                        Sửa
                      </Button>
                    )}
                    {canRequesterCancel(r.status, r.startDate, todayIso) && (
                      <Button variant="outline" size="sm" onClick={() => setCancelId(r.id)}>
                        Hủy
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <RequestDetailDialog requestId={detailId} onClose={() => setDetailId(null)} />
      <EditRequestDialog request={editRequest} onClose={() => setEditRequest(null)} />

      <Dialog open={cancelId != null} onOpenChange={(o) => !o && setCancelId(null)}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Hủy đơn nghỉ phép?</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Đơn sẽ bị hủy. Nếu đơn đã được duyệt, số ngày phép sẽ được hoàn lại. Hành động này
            không thể hoàn tác.
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelId(null)}>
              Đóng
            </Button>
            <Button
              variant="destructive"
              disabled={cancel.isPending}
              onClick={() => {
                if (cancelId == null) return;
                cancel.mutate({ id: cancelId }, { onSuccess: () => setCancelId(null) });
              }}
            >
              {cancel.isPending ? "Đang hủy…" : "Xác nhận hủy"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
