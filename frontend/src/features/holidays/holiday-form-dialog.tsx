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
import type { Holiday } from "@/features/calendar/types";

import { useCreateHoliday, useUpdateHoliday } from "./hooks";

const schema = z.object({
  holidayDate: z.string().min(1, "Chọn ngày"),
  name: z.string().min(1, "Nhập tên ngày lễ").max(200),
  description: z.string().max(1000).optional(),
});

type FormValues = z.infer<typeof schema>;

export function HolidayFormDialog({
  open,
  holiday,
  onClose,
}: {
  open: boolean;
  holiday: Holiday | null;
  onClose: () => void;
}) {
  const isEdit = holiday != null;
  const create = useCreateHoliday();
  const update = useUpdateHoliday();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset({
      holidayDate: holiday?.holidayDate ?? "",
      name: holiday?.name ?? "",
      description: holiday?.description ?? "",
    });
  }, [open, holiday, reset]);

  const onSubmit = handleSubmit((values) => {
    const body = {
      holidayDate: values.holidayDate,
      name: values.name,
      description: values.description || null,
    };
    if (isEdit && holiday) update.mutate({ id: holiday.id, body }, { onSuccess: onClose });
    else create.mutate(body, { onSuccess: onClose });
  });

  const pending = create.isPending || update.isPending;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa ngày lễ" : "Thêm ngày lễ"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label>Ngày</Label>
            <Input type="date" {...register("holidayDate")} />
            {errors.holidayDate && (
              <p className="text-xs text-destructive">{errors.holidayDate.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label>Tên ngày lễ</Label>
            <Input {...register("name")} />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>Mô tả (tùy chọn)</Label>
            <Input {...register("description")} />
          </div>
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
