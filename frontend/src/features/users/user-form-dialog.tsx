import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useMemo } from "react";
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

import { useCreateUser, useDepartmentOptions, useManagerOptions, useUpdateUser } from "./hooks";
import { ROLES, ROLE_LABELS, type User } from "./types";

const baseShape = {
  employeeCode: z
    .string()
    .min(2, "Tối thiểu 2 ký tự")
    .max(50)
    .regex(/^[A-Za-z0-9_-]+$/, "Chỉ gồm chữ, số, _ và -"),
  email: z.string().email("Email không hợp lệ"),
  fullName: z.string().min(1, "Nhập họ tên").max(200),
  role: z.enum(["EMPLOYEE", "MANAGER", "HR", "ADMIN"]),
  departmentId: z.coerce.number().int().positive("Chọn phòng ban"),
  managerId: z.string(),
  joinDate: z.string().min(1, "Chọn ngày vào làm"),
};

function makeSchema(requirePassword: boolean) {
  return z.object({
    ...baseShape,
    password: requirePassword ? z.string().min(8, "Mật khẩu tối thiểu 8 ký tự") : z.string().optional(),
  });
}

export function UserFormDialog({
  open,
  user,
  onClose,
}: {
  open: boolean;
  user: User | null;
  onClose: () => void;
}) {
  const isEdit = user != null;
  const { data: depts } = useDepartmentOptions();
  const { data: managers } = useManagerOptions();
  const create = useCreateUser();
  const update = useUpdateUser();

  const schema = useMemo(() => makeSchema(!isEdit), [isEdit]);
  type FormValues = z.infer<ReturnType<typeof makeSchema>>;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset({
      employeeCode: user?.employeeCode ?? "",
      email: user?.email ?? "",
      fullName: user?.fullName ?? "",
      role: user?.role ?? "EMPLOYEE",
      departmentId: user?.departmentId ?? ("" as unknown as number),
      managerId: user?.managerId != null ? String(user.managerId) : "",
      joinDate: user?.joinDate ?? "",
      password: "",
    });
  }, [open, user, reset]);

  const onSubmit = handleSubmit((values) => {
    const managerId = values.managerId ? Number(values.managerId) : null;
    const common = {
      employeeCode: values.employeeCode,
      email: values.email,
      fullName: values.fullName,
      role: values.role,
      departmentId: values.departmentId,
      managerId,
      joinDate: values.joinDate,
    };
    if (isEdit && user) {
      update.mutate({ id: user.id, body: common }, { onSuccess: onClose });
    } else {
      create.mutate({ ...common, password: values.password as string }, { onSuccess: onClose });
    }
  });

  const pending = create.isPending || update.isPending;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "Sửa người dùng" : "Thêm người dùng"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={onSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Mã nhân viên" error={errors.employeeCode?.message}>
              <Input {...register("employeeCode")} />
            </Field>
            <Field label="Họ tên" error={errors.fullName?.message}>
              <Input {...register("fullName")} />
            </Field>
            <Field label="Email" error={errors.email?.message}>
              <Input type="email" {...register("email")} />
            </Field>
            <Field label="Ngày vào làm" error={errors.joinDate?.message}>
              <Input type="date" {...register("joinDate")} />
            </Field>
            <Field label="Vai trò" error={errors.role?.message}>
              <Select {...register("role")}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {ROLE_LABELS[r]}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Phòng ban" error={errors.departmentId?.message}>
              <Select defaultValue="" {...register("departmentId")}>
                <option value="" disabled>
                  Chọn phòng ban
                </option>
                {depts?.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Quản lý duyệt đơn" error={errors.managerId?.message}>
              <Select {...register("managerId")}>
                <option value="">(Không có)</option>
                {managers
                  ?.filter((m) => m.id !== user?.id)
                  .map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.fullName} ({ROLE_LABELS[m.role]})
                    </option>
                  ))}
              </Select>
            </Field>
            {!isEdit && (
              <Field label="Mật khẩu" error={errors.password?.message}>
                <Input type="password" autoComplete="new-password" {...register("password")} />
              </Field>
            )}
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

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
