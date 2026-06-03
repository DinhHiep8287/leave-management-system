import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

import { useApprove, useReject } from "./hooks";

export type DecisionMode = "approve" | "reject";

export function DecisionDialog({
  mode,
  requestId,
  onClose,
}: {
  mode: DecisionMode | null;
  requestId: number | null;
  onClose: () => void;
}) {
  const open = mode != null && requestId != null;
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const approve = useApprove();
  const reject = useReject();
  const pending = approve.isPending || reject.isPending;

  // Reset the form whenever a new request/mode is opened.
  useEffect(() => {
    if (open) {
      setComment("");
      setError(null);
    }
  }, [open, requestId, mode]);

  const isReject = mode === "reject";

  const onConfirm = () => {
    if (requestId == null || mode == null) return;
    if (isReject && comment.trim() === "") {
      setError("Cần nhập lý do khi từ chối");
      return;
    }
    if (isReject) {
      reject.mutate({ id: requestId, comment: comment.trim() }, { onSuccess: onClose });
    } else {
      approve.mutate(
        { id: requestId, comment: comment.trim() || undefined },
        { onSuccess: onClose },
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isReject ? "Từ chối đơn" : "Duyệt đơn"}</DialogTitle>
          <DialogDescription>
            {isReject
              ? "Nêu rõ lý do từ chối để nhân viên nắm được."
              : "Xác nhận duyệt đơn nghỉ phép này. Quỹ phép sẽ được trừ tương ứng."}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-1.5">
          <Label htmlFor="decision-comment">
            {isReject ? "Lý do từ chối" : "Ghi chú (tùy chọn)"}
          </Label>
          <Textarea
            id="decision-comment"
            rows={3}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder={isReject ? "Lý do từ chối" : "Ghi chú thêm nếu cần"}
          />
          {error && <p className="text-xs text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Đóng
          </Button>
          <Button
            variant={isReject ? "destructive" : "default"}
            disabled={pending}
            onClick={onConfirm}
          >
            {pending ? "Đang xử lý…" : isReject ? "Xác nhận từ chối" : "Xác nhận duyệt"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
