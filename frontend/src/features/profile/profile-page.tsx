import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ROLE_LABELS } from "@/features/users/types";
import { formatDate } from "@/lib/format";

import { useChangeMyPassword, useMyProfile, useUpdateMyName } from "./hooks";

const nameSchema = z.object({ fullName: z.string().min(1, "Nhập họ tên").max(200) });
type NameValues = z.infer<typeof nameSchema>;

const pwSchema = z
  .object({
    oldPassword: z.string().min(1, "Nhập mật khẩu hiện tại"),
    newPassword: z.string().min(8, "Mật khẩu mới tối thiểu 8 ký tự"),
    confirm: z.string(),
  })
  .refine((d) => d.newPassword === d.confirm, {
    message: "Xác nhận mật khẩu không khớp",
    path: ["confirm"],
  });
type PwValues = z.infer<typeof pwSchema>;

export function ProfilePage() {
  const { data: profile } = useMyProfile();
  const updateName = useUpdateMyName();
  const changePw = useChangeMyPassword();

  const nameForm = useForm<NameValues>({ resolver: zodResolver(nameSchema), defaultValues: { fullName: "" } });
  const pwForm = useForm<PwValues>({
    resolver: zodResolver(pwSchema),
    defaultValues: { oldPassword: "", newPassword: "", confirm: "" },
  });

  useEffect(() => {
    if (profile) nameForm.reset({ fullName: profile.fullName });
  }, [profile, nameForm]);

  const onSaveName = nameForm.handleSubmit((v) => updateName.mutate(v.fullName));
  const onChangePw = pwForm.handleSubmit((v) =>
    changePw.mutate(
      { oldPassword: v.oldPassword, newPassword: v.newPassword },
      { onSuccess: () => pwForm.reset({ oldPassword: "", newPassword: "", confirm: "" }) },
    ),
  );

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Hồ sơ cá nhân</h1>
        <p className="text-sm text-muted-foreground">Cập nhật thông tin và mật khẩu của bạn.</p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Thông tin</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSaveName} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <ReadOnly label="Mã nhân viên" value={profile?.employeeCode} />
              <ReadOnly label="Email" value={profile?.email} />
              <ReadOnly label="Vai trò" value={profile ? ROLE_LABELS[profile.role] : ""} />
              <ReadOnly label="Ngày vào làm" value={profile ? formatDate(profile.joinDate) : ""} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="fullName">Họ tên</Label>
              <Input id="fullName" {...nameForm.register("fullName")} />
              {nameForm.formState.errors.fullName && (
                <p className="text-xs text-destructive">{nameForm.formState.errors.fullName.message}</p>
              )}
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={updateName.isPending}>
                {updateName.isPending ? "Đang lưu…" : "Lưu"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Đổi mật khẩu</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onChangePw} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="oldPassword">Mật khẩu hiện tại</Label>
              <Input id="oldPassword" type="password" autoComplete="current-password" {...pwForm.register("oldPassword")} />
              {pwForm.formState.errors.oldPassword && (
                <p className="text-xs text-destructive">{pwForm.formState.errors.oldPassword.message}</p>
              )}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="newPassword">Mật khẩu mới</Label>
                <Input id="newPassword" type="password" autoComplete="new-password" {...pwForm.register("newPassword")} />
                {pwForm.formState.errors.newPassword && (
                  <p className="text-xs text-destructive">{pwForm.formState.errors.newPassword.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="confirm">Xác nhận mật khẩu mới</Label>
                <Input id="confirm" type="password" autoComplete="new-password" {...pwForm.register("confirm")} />
                {pwForm.formState.errors.confirm && (
                  <p className="text-xs text-destructive">{pwForm.formState.errors.confirm.message}</p>
                )}
              </div>
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={changePw.isPending}>
                {changePw.isPending ? "Đang lưu…" : "Đổi mật khẩu"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function ReadOnly({ label, value }: { label: string; value?: string }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      <p className="text-sm text-muted-foreground">{value ?? "…"}</p>
    </div>
  );
}
