import { api } from "@/lib/api";

import type { Department, DepartmentRequest } from "./types";

type Envelope<T> = {
  data: T;
  meta?: { page: number; size: number; totalElements: number; totalPages: number };
};
type PageData<T> = { content: T[]; number: number; totalElements: number; totalPages: number };

export type DeptListResult = {
  items: Department[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export async function listDepartments(
  q: string,
  activeOnly: boolean,
  page: number,
): Promise<DeptListResult> {
  const res = await api.get<Envelope<PageData<Department>>>("/departments", {
    params: { q: q || undefined, activeOnly, page, size: 20 },
  });
  return {
    items: res.data.data.content,
    page: res.data.meta?.page ?? res.data.data.number,
    totalPages: res.data.meta?.totalPages ?? res.data.data.totalPages,
    totalElements: res.data.meta?.totalElements ?? res.data.data.totalElements,
  };
}

export async function createDepartment(body: DepartmentRequest): Promise<Department> {
  const res = await api.post<Envelope<Department>>("/departments", body);
  return res.data.data;
}

export async function updateDepartment(id: number, body: DepartmentRequest): Promise<Department> {
  const res = await api.put<Envelope<Department>>(`/departments/${id}`, body);
  return res.data.data;
}

export async function deleteDepartment(id: number): Promise<void> {
  await api.delete(`/departments/${id}`);
}
