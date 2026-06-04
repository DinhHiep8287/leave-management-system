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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { LeaveBalance } from "@/features/dashboard/types";

import { useAdjustBalance } from "./hooks";

export function AdjustBalanceDialog({
  balance,
  onClose,
}: {
  balance: LeaveBalance | null;
  onClose: () => void;
}) {
  const open = balance != null;
  const [delta, setDelta] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const adjust = useAdjustBalance();

  useEffect(() => {
    if (open) {
      setDelta("");
      setReason("");
      setError(null);
    }
  }, [open, balance]);

  const submit = () => {
    const d = Number(delta);
    if (delta.trim() === "" || Number.isNaN(d)) {
      setError("Nhập số ngày điều chỉnh (có thể âm)");
      return;
    }
    if (reason.trim() === "") {
      setError("Nhập lý do điều chỉnh");
      return;
    }
    if (balance) {
      adjust.mutate({ id: balance.id, delta: d, reason: reason.trim() }, { onSuccess: onClose });
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Điều chỉnh quỹ phép</DialogTitle>
          <DialogDescription>
            {balance?.leaveTypeCode} · còn lại hiện tại {balance?.remainingDays}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="delta">Số ngày điều chỉnh (+/-)</Label>
            <Input
              id="delta"
              type="number"
              step="0.5"
              value={delta}
              onChange={(e) => setDelta(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="reason">Lý do</Label>
            <Input id="reason" value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={adjust.isPending} onClick={submit}>
            {adjust.isPending ? "Đang lưu…" : "Lưu"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
