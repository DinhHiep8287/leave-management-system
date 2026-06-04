import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import { createHoliday, deleteHoliday, updateHoliday, type HolidayRequest } from "./api";

function invalidate(qc: ReturnType<typeof useQueryClient>) {
  // Shared with the calendar's holiday shading (queryKey ["holidays", year]).
  void qc.invalidateQueries({ queryKey: ["holidays"] });
}

export function useCreateHoliday() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: HolidayRequest) => createHoliday(body),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã thêm ngày lễ");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Thêm ngày lễ thất bại")),
  });
}

export function useUpdateHoliday() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: HolidayRequest }) => updateHoliday(id, body),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã cập nhật ngày lễ");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Cập nhật thất bại")),
  });
}

export function useDeleteHoliday() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteHoliday(id),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã xóa ngày lễ");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Xóa thất bại")),
  });
}
