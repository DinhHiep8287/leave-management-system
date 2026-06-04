import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import { changeMyPassword, getMyProfile, updateMyName } from "./api";

export function useMyProfile() {
  return useQuery({ queryKey: ["my-profile"], queryFn: getMyProfile });
}

export function useUpdateMyName() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (fullName: string) => updateMyName(fullName),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["my-profile"] });
      toast.success("Đã cập nhật hồ sơ");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Cập nhật thất bại")),
  });
}

export function useChangeMyPassword() {
  return useMutation({
    mutationFn: ({ oldPassword, newPassword }: { oldPassword: string; newPassword: string }) =>
      changeMyPassword(oldPassword, newPassword),
    onSuccess: () => toast.success("Đã đổi mật khẩu"),
    onError: (e) => toast.error(apiErrorMessage(e, "Đổi mật khẩu thất bại")),
  });
}
