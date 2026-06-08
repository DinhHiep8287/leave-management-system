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

export type DepartmentLeaveCount = {
  departmentId: number;
  departmentName: string | null;
  requestCount: number;
};

export type AdminSummary = {
  totalActiveEmployees: number;
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  cancelledCount: number;
  topDepartmentsThisMonth: DepartmentLeaveCount[];
};

export type DashboardSummary = {
  pendingApprovalCount: number;
  onLeaveTodayCount: number;
  onLeaveThisWeekCount: number;
  myPendingCount: number;
  myBalances: LeaveBalance[];
  onLeaveToday: CalendarEntry[];
};
