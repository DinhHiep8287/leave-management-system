import { api } from "@/lib/api";

import type {
  AttachmentResponse,
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

export async function updateRequest(
  id: number,
  body: LeaveRequestCreateRequest,
): Promise<LeaveRequestResponse> {
  const res = await api.put<Envelope<LeaveRequestResponse>>(`/leave-requests/${id}`, body);
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

export async function getAttachments(requestId: number): Promise<AttachmentResponse[]> {
  const res = await api.get<Envelope<AttachmentResponse[]>>(
    `/leave-requests/${requestId}/attachments`,
  );
  return res.data.data;
}

export async function uploadAttachments(
  requestId: number,
  files: File[],
): Promise<AttachmentResponse[]> {
  const form = new FormData();
  files.forEach((file) => form.append("files", file));
  const res = await api.post<Envelope<AttachmentResponse[]>>(
    `/leave-requests/${requestId}/attachments`,
    form,
  );
  return res.data.data;
}

export async function deleteAttachment(requestId: number, attachmentId: number): Promise<void> {
  await api.delete(`/leave-requests/${requestId}/attachments/${attachmentId}`);
}

export async function downloadAttachment(
  requestId: number,
  attachmentId: number,
): Promise<Blob> {
  const res = await api.get(`/leave-requests/${requestId}/attachments/${attachmentId}/download`, {
    responseType: "blob",
  });
  return res.data as Blob;
}
