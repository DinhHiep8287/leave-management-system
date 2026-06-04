import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import {
  cancelRequest,
  getHistory,
  getRequest,
  getMyRequests,
  listLeaveTypes,
  submitLeaveRequest,
  updateRequest,
} from "./api";
import type { LeaveRequestCreateRequest, LeaveStatus } from "./types";

export function useLeaveTypes(activeOnly = true) {
  return useQuery({
    queryKey: ["leave-types", activeOnly],
    queryFn: () => listLeaveTypes(activeOnly),
    staleTime: 5 * 60_000,
  });
}

export function useMyRequests(userId: number | undefined, year?: number, status?: LeaveStatus) {
  return useQuery({
    queryKey: ["my-requests", userId, year ?? null, status ?? null],
    queryFn: () => getMyRequests(userId as number, year, status),
    enabled: userId != null,
  });
}

export function useRequest(id: number | undefined) {
  return useQuery({
    queryKey: ["leave-request", id],
    queryFn: () => getRequest(id as number),
    enabled: id != null,
  });
}

export function useHistory(id: number | undefined, enabled = true) {
  return useQuery({
    queryKey: ["leave-request-history", id],
    queryFn: () => getHistory(id as number),
    enabled: id != null && enabled,
  });
}

export function useSubmitLeaveRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: LeaveRequestCreateRequest) => submitLeaveRequest(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["my-requests"] });
      void qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success("Đã nộp đơn nghỉ phép");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Nộp đơn thất bại")),
  });
}

export function useUpdateRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: LeaveRequestCreateRequest }) =>
      updateRequest(id, body),
    onSuccess: (updated) => {
      void qc.invalidateQueries({ queryKey: ["my-requests"] });
      void qc.invalidateQueries({ queryKey: ["leave-request", updated.id] });
      void qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success("Đã cập nhật đơn");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Cập nhật đơn thất bại")),
  });
}

export function useCancelRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, comment }: { id: number; comment?: string }) => cancelRequest(id, comment),
    onSuccess: (updated) => {
      void qc.invalidateQueries({ queryKey: ["my-requests"] });
      void qc.invalidateQueries({ queryKey: ["leave-request", updated.id] });
      void qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success("Đã hủy đơn");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Hủy đơn thất bại")),
  });
}
