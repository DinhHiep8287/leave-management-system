import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { useResetPassword } from "./hooks";
import type { User } from "./types";

export function ResetPasswordDialog({ user, onClose }: { user: User | null; onClose: () => void }) {
  const open = user != null;
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const reset = useResetPassword();

  useEffect(() => {
    if (open) {
      setPassword("");
      setError(null);
    }
  }, [open, user]);

  const submit = () => {
    if (password.length < 8) {
      setError("Mật khẩu tối thiểu 8 ký tự");
      return;
    }
    if (user) reset.mutate({ id: user.id, newPassword: password }, { onSuccess: onClose });
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Đặt lại mật khẩu</DialogTitle>
        </DialogHeader>
        <div className="space-y-1.5">
          <Label htmlFor="new-password">Mật khẩu mới cho {user?.fullName}</Label>
          <Input
            id="new-password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={reset.isPending} onClick={submit}>
            {reset.isPending ? "Đang lưu…" : "Đặt lại"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
