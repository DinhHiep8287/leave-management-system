import type { CalendarEntry } from "@/features/calendar/types";

export type LeaveBalance = {
  id: number;
  userId: number;
  userFullName: string;
  leaveTypeId: number;
  leaveTypeCode: string;
  year: number;
  totalDays: number;
  usedDays: number;
  adjustedDays: number;
  remainingDays: number;
};

export type DashboardSummary = {
  pendingApprovalCount: number;
  onLeaveTodayCount: number;
  myPendingCount: number;
  myBalances: LeaveBalance[];
  onLeaveToday: CalendarEntry[];
};
