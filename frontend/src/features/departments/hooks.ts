import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import { createDepartment, deleteDepartment, listDepartments, updateDepartment } from "./api";
import type { DepartmentRequest } from "./types";

export function useDepartments(q: string, activeOnly: boolean, page: number) {
  return useQuery({
    queryKey: ["departments", q, activeOnly, page],
    queryFn: () => listDepartments(q, activeOnly, page),
    placeholderData: keepPreviousData,
  });
}

function invalidate(qc: ReturnType<typeof useQueryClient>) {
  void qc.invalidateQueries({ queryKey: ["departments"] });
  void qc.invalidateQueries({ queryKey: ["department-options"] });
}

export function useCreateDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: DepartmentRequest) => createDepartment(body),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã tạo phòng ban");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Tạo phòng ban thất bại")),
  });
}

export function useUpdateDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: DepartmentRequest }) => updateDepartment(id, body),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã cập nhật phòng ban");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Cập nhật thất bại")),
  });
}

export function useDeleteDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteDepartment(id),
    onSuccess: () => {
      invalidate(qc);
      toast.success("Đã vô hiệu hóa phòng ban");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Thao tác thất bại")),
  });
}
