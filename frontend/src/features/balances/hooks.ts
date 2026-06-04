import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import { adjustBalance, getUserBalances, initializeYear, listUserOptions } from "./api";

export function useUserOptions() {
  return useQuery({ queryKey: ["user-options"], queryFn: listUserOptions, staleTime: 5 * 60_000 });
}

export function useUserBalances(userId: number | undefined, year: number) {
  return useQuery({
    queryKey: ["user-balances", userId, year],
    queryFn: () => getUserBalances(userId as number, year),
    enabled: userId != null,
  });
}

export function useInitializeYear() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (year: number) => initializeYear(year),
    onSuccess: (r) => {
      void qc.invalidateQueries({ queryKey: ["user-balances"] });
      toast.success(`Đã khởi tạo ${r.created} dòng quỹ phép cho năm ${r.year}`);
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Khởi tạo thất bại")),
  });
}

export function useAdjustBalance() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, delta, reason }: { id: number; delta: number; reason: string }) =>
      adjustBalance(id, delta, reason),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["user-balances"] });
      void qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success("Đã điều chỉnh quỹ phép");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Điều chỉnh thất bại")),
  });
}
