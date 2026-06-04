import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import { createLeaveType, deleteLeaveType, listLeaveTypes, updateLeaveType } from "./api";
import type { LeaveTypeRequest } from "./types";

export function useLeaveTypesAdmin(activeOnly: boolean) {
  return useQuery({
    queryKey: ["leave-types-admin", activeOnly],
    queryFn: () => listLeaveTypes(activeOnly),
  });
}

function invalidate(qc: ReturnType<typeof useQueryClient>) {
  void qc.invalidateQueries({ queryKey: ["leave-types-admin"] });
  void qc.invalidateQueries({ queryKey: ["leave-types"] }); // the submit form's lookup
}

export function useCreateLeaveType() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: LeaveTypeRequest) => createLeaveType(body),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã tạo loại nghỉ phép");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Tạo loại nghỉ phép thất bại")),
  });
}

export function useUpdateLeaveType() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: LeaveTypeRequest }) => updateLeaveType(id, body),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã cập nhật loại nghỉ phép");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Cập nhật thất bại")),
  });
}

export function useDeleteLeaveType() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteLeaveType(id),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã vô hiệu hóa loại nghỉ phép");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Thao tác thất bại")),
  });
}
