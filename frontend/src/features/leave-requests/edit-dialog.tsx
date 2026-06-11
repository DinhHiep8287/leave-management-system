import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";

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
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

import { useLeaveTypes, useUpdateRequest } from "./hooks";
import { HALVES, leaveRequestSchema, type LeaveRequestFormValues } from "./schema";
import { HALF_LABELS, type LeaveRequestResponse } from "./types";

type FormValues = LeaveRequestFormValues;
const schema = leaveRequestSchema;

export function EditRequestDialog({
  request,
  onClose,
}: {
  request: LeaveRequestResponse | null;
  onClose: () => void;
}) {
  const open = request != null;
  const { data: types } = useLeaveTypes(true);
  const update = useUpdateRequest();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (open && request) {
      reset({
        leaveTypeId: request.leaveTypeId,
        startDate: request.startDate,
        endDate: request.endDate,
        startHalf: request.startHalf,
        endHalf: request.endHalf,
        reason: request.reason,
      });
    }
  }, [open, request, reset]);

  const onSubmit = handleSubmit((values) => {
    if (request) update.mutate({ id: request.id, body: values }, { onSuccess: onClose });
  });

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Sửa đơn nghỉ phép</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label>Loại nghỉ phép</Label>
            <Select {...register("leaveTypeId")}>
              {types?.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.code})
                </option>
              ))}
            </Select>
            {errors.leaveTypeId && (
              <p className="text-xs text-destructive">{errors.leaveTypeId.message}</p>
            )}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Ngày bắt đầu</Label>
              <Input type="date" {...register("startDate")} />
              {errors.startDate && (
                <p className="text-xs text-destructive">{errors.startDate.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label>Ngày kết thúc</Label>
              <Input type="date" {...register("endDate")} />
              {errors.endDate && <p className="text-xs text-destructive">{errors.endDate.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label>Buổi (bắt đầu)</Label>
              <Select {...register("startHalf")}>
                {HALVES.map((h) => (
                  <option key={h} value={h}>
                    {HALF_LABELS[h]}
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Buổi (kết thúc)</Label>
              <Select {...register("endHalf")}>
                {HALVES.map((h) => (
                  <option key={h} value={h}>
                    {HALF_LABELS[h]}
                  </option>
                ))}
              </Select>
            </div>
          </div>
          <div className="space-y-1.5">
            <Label>Lý do</Label>
            <Textarea rows={3} {...register("reason")} />
            {errors.reason && <p className="text-xs text-destructive">{errors.reason.message}</p>}
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={update.isPending}>
              {update.isPending ? "Đang lưu…" : "Lưu thay đổi"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
