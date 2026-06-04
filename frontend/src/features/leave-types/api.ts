import { api } from "@/lib/api";

import type { LeaveType, LeaveTypeRequest } from "./types";

type Envelope<T> = { data: T };

export async function listLeaveTypes(activeOnly: boolean): Promise<LeaveType[]> {
  const res = await api.get<Envelope<LeaveType[]>>("/leave-types", { params: { activeOnly } });
  return res.data.data;
}

export async function createLeaveType(body: LeaveTypeRequest): Promise<LeaveType> {
  const res = await api.post<Envelope<LeaveType>>("/leave-types", body);
  return res.data.data;
}

export async function updateLeaveType(id: number, body: LeaveTypeRequest): Promise<LeaveType> {
  const res = await api.put<Envelope<LeaveType>>(`/leave-types/${id}`, body);
  return res.data.data;
}

export async function deleteLeaveType(id: number): Promise<void> {
  await api.delete(`/leave-types/${id}`);
}
