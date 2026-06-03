import { api } from "@/lib/api";
import type { LeaveRequestResponse, LeaveStatus } from "@/features/leave-requests/types";

type PageData<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type Envelope<T> = {
  data: T;
  meta?: { page: number; size: number; totalElements: number; totalPages: number };
};

export type InboxResult = {
  items: LeaveRequestResponse[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export async function getInbox(
  status: LeaveStatus | undefined,
  page: number,
  size = 20,
): Promise<InboxResult> {
  const res = await api.get<Envelope<PageData<LeaveRequestResponse>>>("/leave-requests", {
    params: { status, page, size },
  });
  return {
    items: res.data.data.content,
    page: res.data.meta?.page ?? res.data.data.number,
    totalPages: res.data.meta?.totalPages ?? res.data.data.totalPages,
    totalElements: res.data.meta?.totalElements ?? res.data.data.totalElements,
  };
}

export async function approveRequest(id: number, comment?: string): Promise<LeaveRequestResponse> {
  const res = await api.post<Envelope<LeaveRequestResponse>>(`/leave-requests/${id}/approve`, {
    comment,
  });
  return res.data.data;
}

export async function rejectRequest(id: number, comment: string): Promise<LeaveRequestResponse> {
  const res = await api.post<Envelope<LeaveRequestResponse>>(`/leave-requests/${id}/reject`, {
    comment,
  });
  return res.data.data;
}
