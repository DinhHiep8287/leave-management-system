import { api } from "@/lib/api";

import type { Role, User, UserCreateRequest, UserUpdateRequest } from "./types";

type Envelope<T> = {
  data: T;
  meta?: { page: number; size: number; totalElements: number; totalPages: number };
};
type PageData<T> = { content: T[]; number: number; totalElements: number; totalPages: number };

export type UserListParams = {
  q?: string;
  departmentId?: number;
  role?: Role;
  activeOnly: boolean;
  page: number;
};

export type UserListResult = {
  items: User[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export async function listUsers(params: UserListParams): Promise<UserListResult> {
  const res = await api.get<Envelope<PageData<User>>>("/users", {
    params: {
      q: params.q || undefined,
      departmentId: params.departmentId,
      role: params.role,
      activeOnly: params.activeOnly,
      page: params.page,
      size: 20,
    },
  });
  return {
    items: res.data.data.content,
    page: res.data.meta?.page ?? res.data.data.number,
    totalPages: res.data.meta?.totalPages ?? res.data.data.totalPages,
    totalElements: res.data.meta?.totalElements ?? res.data.data.totalElements,
  };
}

export async function createUser(body: UserCreateRequest): Promise<User> {
  const res = await api.post<Envelope<User>>("/users", body);
  return res.data.data;
}

export async function updateUser(id: number, body: UserUpdateRequest): Promise<User> {
  const res = await api.put<Envelope<User>>(`/users/${id}`, body);
  return res.data.data;
}

export async function resetUserPassword(id: number, newPassword: string): Promise<void> {
  await api.post(`/users/${id}/reset-password`, { newPassword });
}

export async function setUserActive(id: number, active: boolean): Promise<User> {
  const res = await api.post<Envelope<User>>(`/users/${id}/${active ? "activate" : "deactivate"}`, {});
  return res.data.data;
}

// --- lookups for the form ---

export type DeptOption = { id: number; code: string; name: string };

export async function listDepartmentOptions(): Promise<DeptOption[]> {
  const res = await api.get<Envelope<PageData<DeptOption>>>("/departments", {
    params: { size: 100, activeOnly: true },
  });
  return res.data.data.content;
}

export type ManagerOption = { id: number; fullName: string; role: Role };

/** Active users that can be assigned as a manager (manager/HR/admin). */
export async function listManagerOptions(): Promise<ManagerOption[]> {
  const res = await api.get<Envelope<PageData<User>>>("/users", {
    params: { activeOnly: true, size: 200 },
  });
  return res.data.data.content
    .filter((u) => u.role === "MANAGER" || u.role === "HR" || u.role === "ADMIN")
    .map((u) => ({ id: u.id, fullName: u.fullName, role: u.role }));
}
