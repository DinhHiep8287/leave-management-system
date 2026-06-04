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
import { Select } from "@/components/ui/select";
import { ROLE_LABELS } from "@/features/users/types";
import { useManagerOptions } from "@/features/users/hooks";

import { useCreateDepartment, useUpdateDepartment } from "./hooks";
import type { Department } from "./types";

const schema = z.object({
  code: z
    .string()
    .min(2, "Tối thiểu 2 ký tự")
    .max(50)
    .regex(/^[A-Za-z0-9_-]+$/, "Chỉ gồm chữ, số, _ và -"),
  name: z.string().min(1, "Nhập tên phòng ban").max(200),
  headUserId: z.string(),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

export function DepartmentFormDialog({
  open,
  department,
  onClose,
}: {
  open: boolean;
  department: Department | null;
  onClose: () => void;
}) {
  const isEdit = department != null;
  const { data: heads } = useManagerOptions();
  const create = useCreateDepartment();
  const update = useUpdateDepartment();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset({
      code: department?.code ?? "",
      name: department?.name ?? "",
      headUserId: department?.headUserId != null ? String(department.headUserId) : "",
      isActive: department?.active ?? true,
    });
  }, [open, department, reset]);

  const onSubmit = handleSubmit((values) => {
    const body = {
      code: values.code,
      name: values.name,
      headUserId: values.headUserId ? Number(values.headUserId) : null,
      isActive: values.isActive,
    };
    if (isEdit && department) update.mutate({ id: department.id, body }, { onSuccess: onClose });
    else create.mutate(body, { onSuccess: onClose });
  });

  const pending = create.isPending || update.isPending;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa phòng ban" : "Thêm phòng ban"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label>Mã phòng ban</Label>
            <Input {...register("code")} />
            {errors.code && <p className="text-xs text-destructive">{errors.code.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>Tên phòng ban</Label>
            <Input {...register("name")} />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>Trưởng phòng</Label>
            <Select {...register("headUserId")}>
              <option value="">(Không có)</option>
              {heads?.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.fullName} ({ROLE_LABELS[h.role]})
                </option>
              ))}
            </Select>
          </div>
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
