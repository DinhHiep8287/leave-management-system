import type { LeaveHalf, LeaveStatus } from "@/features/leave-requests/types";

export type CalendarEntry = {
  leaveRequestId: number;
  userId: number;
  userFullName: string | null;
  leaveTypeCode: string | null;
  startDate: string;
  endDate: string;
  startHalf: LeaveHalf;
  endHalf: LeaveHalf;
  status: LeaveStatus;
};

export type Holiday = {
  id: number;
  holidayDate: string;
  name: string;
  description: string | null;
};

export type DeptOption = { id: number; code: string; name: string };
