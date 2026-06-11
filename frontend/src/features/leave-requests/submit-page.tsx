import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

import { useLeaveTypes, useSubmitLeaveRequest } from "./hooks";
import { HALVES, leaveRequestSchema, type LeaveRequestFormValues } from "./schema";
import { HALF_LABELS } from "./types";

type FormValues = LeaveRequestFormValues;
const schema = leaveRequestSchema;

export function SubmitLeaveRequestPage() {
  const navigate = useNavigate();
  const { data: types, isLoading: typesLoading } = useLeaveTypes(true);
  const submit = useSubmitLeaveRequest();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { startHalf: "FULL_DAY", endHalf: "FULL_DAY", reason: "" },
  });

  const onSubmit = handleSubmit((values) => {
    submit.mutate(values, { onSuccess: () => navigate("/leave-requests") });
  });

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Nộp đơn nghỉ phép</h1>
        <p className="text-sm text-muted-foreground">
          Số ngày nghỉ được tính tự động, trừ cuối tuần và ngày lễ.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle>Thông tin đơn</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <Label htmlFor="leaveTypeId">Loại nghỉ phép</Label>
              <Select id="leaveTypeId" defaultValue="" disabled={typesLoading} {...register("leaveTypeId")}>
                <option value="" disabled>
                  {typesLoading ? "Đang tải…" : "Chọn loại nghỉ phép"}
                </option>
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
                <Label htmlFor="startDate">Ngày bắt đầu</Label>
                <Input id="startDate" type="date" {...register("startDate")} />
                {errors.startDate && (
                  <p className="text-xs text-destructive">{errors.startDate.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="endDate">Ngày kết thúc</Label>
                <Input id="endDate" type="date" {...register("endDate")} />
                {errors.endDate && (
                  <p className="text-xs text-destructive">{errors.endDate.message}</p>
                )}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="startHalf">Buổi (ngày bắt đầu)</Label>
                <Select id="startHalf" {...register("startHalf")}>
                  {HALVES.map((h) => (
                    <option key={h} value={h}>
                      {HALF_LABELS[h]}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="endHalf">Buổi (ngày kết thúc)</Label>
                <Select id="endHalf" {...register("endHalf")}>
                  {HALVES.map((h) => (
                    <option key={h} value={h}>
                      {HALF_LABELS[h]}
                    </option>
                  ))}
                </Select>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="reason">Lý do</Label>
              <Textarea id="reason" rows={3} placeholder="Nêu lý do nghỉ phép" {...register("reason")} />
              {errors.reason && <p className="text-xs text-destructive">{errors.reason.message}</p>}
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => navigate("/leave-requests")}>
                Hủy
              </Button>
              <Button type="submit" disabled={submit.isPending}>
                {submit.isPending ? "Đang gửi…" : "Nộp đơn"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
