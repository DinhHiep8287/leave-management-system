import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

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
import { Textarea } from "@/components/ui/textarea";

import { useCreateLeaveType, useUpdateLeaveType } from "./hooks";
import type { LeaveType } from "./types";

const schema = z.object({
  code: z
    .string()
    .min(2, "Tối thiểu 2 ký tự")
    .max(50)
    .regex(/^[A-Za-z0-9_-]+$/, "Chỉ gồm chữ, số, _ và -"),
  name: z.string().min(1, "Nhập tên").max(200),
  description: z.string().max(2000).optional(),
  defaultQuotaDays: z.coerce.number().min(0, "Không âm").max(366),
  requiresBalance: z.boolean(),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

export function LeaveTypeFormDialog({
  open,
  leaveType,
  onClose,
}: {
  open: boolean;
  leaveType: LeaveType | null;
  onClose: () => void;
}) {
  const isEdit = leaveType != null;
  const create = useCreateLeaveType();
  const update = useUpdateLeaveType();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset({
      code: leaveType?.code ?? "",
      name: leaveType?.name ?? "",
      description: leaveType?.description ?? "",
      defaultQuotaDays: leaveType?.defaultQuotaDays ?? 0,
      requiresBalance: leaveType?.requiresBalance ?? true,
      isActive: leaveType?.active ?? true,
    });
  }, [open, leaveType, reset]);

  const onSubmit = handleSubmit((values) => {
    const body = {
      code: values.code,
      name: values.name,
      description: values.description || null,
      defaultQuotaDays: values.defaultQuotaDays,
      requiresBalance: values.requiresBalance,
      isActive: values.isActive,
    };
    if (isEdit && leaveType) update.mutate({ id: leaveType.id, body }, { onSuccess: onClose });
    else create.mutate(body, { onSuccess: onClose });
  });

  const pending = create.isPending || update.isPending;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa loại nghỉ phép" : "Thêm loại nghỉ phép"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Mã</Label>
              <Input {...register("code")} />
              {errors.code && <p className="text-xs text-destructive">{errors.code.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label>Tên</Label>
              <Input {...register("name")} />
              {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
            </div>
          </div>
          <div className="space-y-1.5">
            <Label>Mô tả</Label>
            <Textarea rows={2} {...register("description")} />
          </div>
          <div className="space-y-1.5">
            <Label>Quota mặc định (ngày/năm)</Label>
            <Input type="number" step="0.5" min="0" {...register("defaultQuotaDays")} />
            {errors.defaultQuotaDays && (
              <p className="text-xs text-destructive">{errors.defaultQuotaDays.message}</p>
            )}
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 rounded border-input" {...register("requiresBalance")} />
            Trừ vào quỹ phép (requires balance)
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 rounded border-input" {...register("isActive")} />
            Đang hoạt động
          </label>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={pending}>
              {pending ? "Đang lưu…" : isEdit ? "Lưu" : "Tạo"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
