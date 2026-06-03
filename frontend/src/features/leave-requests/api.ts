import { api } from "@/lib/api";

import type {
  ApprovalActionResponse,
  LeaveRequestCreateRequest,
  LeaveRequestResponse,
  LeaveStatus,
  LeaveType,
} from "./types";

type Envelope<T> = { data: T; meta?: Record<string, unknown> };

export async function listLeaveTypes(activeOnly = true): Promise<LeaveType[]> {
  const res = await api.get<Envelope<LeaveType[]>>("/leave-types", { params: { activeOnly } });
  return res.data.data;
}

export async function submitLeaveRequest(
  body: LeaveRequestCreateRequest,
): Promise<LeaveRequestResponse> {
  const res = await api.post<Envelope<LeaveRequestResponse>>("/leave-requests", body);
  return res.data.data;
}

export async function getMyRequests(
  userId: number,
  year?: number,
  status?: LeaveStatus,
): Promise<LeaveRequestResponse[]> {
  const res = await api.get<Envelope<LeaveRequestResponse[]>>(`/users/${userId}/leave-requests`, {
    params: { year, status },
  });
  return res.data.data;
}

export async function getRequest(id: number): Promise<LeaveRequestResponse> {
  const res = await api.get<Envelope<LeaveRequestResponse>>(`/leave-requests/${id}`);
  return res.data.data;
}

export async function cancelRequest(id: number, comment?: string): Promise<LeaveRequestResponse> {
  const res = await api.post<Envelope<LeaveRequestResponse>>(`/leave-requests/${id}/cancel`, {
    comment,
  });
  return res.data.data;
}

export async function getHistory(id: number): Promise<ApprovalActionResponse[]> {
  const res = await api.get<Envelope<ApprovalActionResponse[]>>(`/leave-requests/${id}/history`);
  return res.data.data;
}
