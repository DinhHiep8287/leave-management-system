import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { apiErrorMessage } from "@/lib/api-error";

import {
  createUser,
  listDepartmentOptions,
  listManagerOptions,
  listUsers,
  resetUserPassword,
  setUserActive,
  updateUser,
  type UserListParams,
} from "./api";
import type { UserCreateRequest, UserUpdateRequest } from "./types";

export function useUsers(params: UserListParams) {
  return useQuery({
    queryKey: ["users", params],
    queryFn: () => listUsers(params),
    placeholderData: keepPreviousData,
  });
}

export function useDepartmentOptions() {
  return useQuery({
    queryKey: ["department-options"],
    queryFn: listDepartmentOptions,
    staleTime: 30 * 60_000,
  });
}

export function useManagerOptions() {
  return useQuery({
    queryKey: ["manager-options"],
    queryFn: listManagerOptions,
    staleTime: 5 * 60_000,
  });
}

export function useCreateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UserCreateRequest) => createUser(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["users"] });
      void qc.invalidateQueries({ queryKey: ["manager-options"] });
      toast.success("Đã tạo người dùng");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Tạo người dùng thất bại")),
  });
}

export function useUpdateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UserUpdateRequest }) => updateUser(id, body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["users"] });
      void qc.invalidateQueries({ queryKey: ["manager-options"] });
      toast.success("Đã cập nhật người dùng");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Cập nhật thất bại")),
  });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: ({ id, newPassword }: { id: number; newPassword: string }) =>
      resetUserPassword(id, newPassword),
    onSuccess: () => toast.success("Đã đặt lại mật khẩu"),
    onError: (e) => toast.error(apiErrorMessage(e, "Đặt lại mật khẩu thất bại")),
  });
}

export function useSetUserActive() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => setUserActive(id, active),
    onSuccess: (u) => {
      void qc.invalidateQueries({ queryKey: ["users"] });
      toast.success(u.active ? "Đã kích hoạt" : "Đã khóa tài khoản");
    },
    onError: (e) => toast.error(apiErrorMessage(e, "Thao tác thất bại")),
  });
}
