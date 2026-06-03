import { StatusBadge } from "@/components/status-badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { formatDate, formatDateTime } from "@/lib/format";

import { useHistory, useRequest } from "./hooks";
import { HALF_LABELS } from "./types";

const ACTION_LABELS: Record<string, string> = {
  CREATED: "Tạo đơn",
  UPDATED: "Cập nhật",
  APPROVED: "Duyệt",
  REJECTED: "Từ chối",
  CANCELLED: "Hủy",
  OVERRIDE: "Ghi đè",
};

export function RequestDetailDialog({
  requestId,
  onClose,
}: {
  requestId: number | null;
  onClose: () => void;
}) {
  const open = requestId != null;
  const { data: req, isLoading } = useRequest(requestId ?? undefined);
  const { data: history } = useHistory(requestId ?? undefined, open);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Chi tiết đơn nghỉ phép</DialogTitle>
          <DialogDescription>Thông tin đơn và lịch sử xử lý.</DialogDescription>
        </DialogHeader>

        {isLoading || !req ? (
          <p className="text-sm text-muted-foreground">Đang tải…</p>
        ) : (
          <div className="space-y-5 text-sm">
            <dl className="grid grid-cols-3 gap-y-2">
              <Row label="Trạng thái">
                <StatusBadge status={req.status} />
              </Row>
              <Row label="Loại">{req.leaveTypeCode}</Row>
              <Row label="Số ngày">{req.totalDays}</Row>
              <Row label="Từ ngày">
                {formatDate(req.startDate)} · {HALF_LABELS[req.startHalf]}
              </Row>
              <Row label="Đến ngày">
                {formatDate(req.endDate)} · {HALF_LABELS[req.endHalf]}
              </Row>
              <Row label="Quản lý">{req.managerName ?? "(chưa gán)"}</Row>
              <Row label="Lý do">{req.reason}</Row>
            </dl>

            <div>
              <h4 className="mb-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Lịch sử
              </h4>
              <ol className="space-y-3 border-l border-border pl-4">
                {history?.map((h) => (
                  <li key={h.id} className="relative">
                    <span className="absolute -left-[1.30rem] top-1 h-2 w-2 rounded-full bg-primary" />
                    <p className="font-medium">{ACTION_LABELS[h.action] ?? h.action}</p>
                    <p className="text-xs text-muted-foreground">
                      {h.actorName ?? "Hệ thống"} · {formatDateTime(h.createdAt)}
                    </p>
                    {h.comment && <p className="mt-0.5 text-xs">{h.comment}</p>}
                  </li>
                ))}
                {history && history.length === 0 && (
                  <li className="text-xs text-muted-foreground">Chưa có hoạt động.</li>
                )}
              </ol>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <>
      <dt className="col-span-1 text-muted-foreground">{label}</dt>
      <dd className="col-span-2">{children}</dd>
    </>
  );
}
