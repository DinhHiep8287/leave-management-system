import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import type { LeaveStatus } from "@/features/leave-requests/types";
import { apiErrorMessage } from "@/lib/api-error";

import { approveRequest, getInbox, rejectRequest } from "./api";

export function useInbox(status: LeaveStatus | undefined, page: number) {
  return useQuery({
    queryKey: ["inbox", status ?? null, page],
    queryFn: () => getInbox(status, page),
    placeholderData: keepPreviousData,
  });
}

export function useApprove() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, comment }: { id: number; comment?: string }) => approveRequest(id, comment),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["inbox"] });
      void qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success("Đã duyệt đơn");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Duyệt đơn thất bại")),
  });
}

export function useReject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, comment }: { id: number; comment: string }) => rejectRequest(id, comment),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["inbox"] });
      void qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success("Đã từ chối đơn");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Từ chối đơn thất bại")),
  });
}
