export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type LeaveHalf = "FULL_DAY" | "MORNING" | "AFTERNOON";

export type LeaveRequestResponse = {
  id: number;
  userId: number;
  userFullName: string | null;
  leaveTypeId: number;
  leaveTypeCode: string | null;
  startDate: string;
  endDate: string;
  startHalf: LeaveHalf;
  endHalf: LeaveHalf;
  totalDays: number;
  reason: string;
  status: LeaveStatus;
  managerId: number | null;
  managerName: string | null;
  createdAt: string;
};

export type LeaveRequestCreateRequest = {
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  startHalf: LeaveHalf;
  endHalf: LeaveHalf;
  reason: string;
};

export type AttachmentResponse = {
  id: number;
  leaveRequestId: number;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedById: number;
  uploadedByName: string | null;
  createdAt: string;
};

export type ApprovalActionResponse = {
  id: number;
  action: string;
  actorId: number;
  actorName: string | null;
  previousStatus: LeaveStatus | null;
  newStatus: LeaveStatus | null;
  comment: string | null;
  createdAt: string;
};

export type LeaveType = {
  id: number;
  code: string;
  name: string;
  description: string | null;
  defaultQuotaDays: number;
  requiresBalance: boolean;
  active: boolean;
};

export const STATUS_LABELS: Record<LeaveStatus, string> = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  CANCELLED: "Đã hủy",
};

export const HALF_LABELS: Record<LeaveHalf, string> = {
  FULL_DAY: "Cả ngày",
  MORNING: "Buổi sáng",
  AFTERNOON: "Buổi chiều",
};
