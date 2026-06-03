package com.peih68.leave.dashboard.web.dto;

import com.peih68.leave.leavebalance.web.dto.LeaveBalanceResponse;
import com.peih68.leave.leaverequest.web.dto.CalendarEntryResponse;
import java.util.List;

/** At-a-glance figures for the authenticated user's home dashboard. */
public record DashboardSummaryResponse(
        long pendingApprovalCount,
        int onLeaveTodayCount,
        long myPendingCount,
        List<LeaveBalanceResponse> myBalances,
        List<CalendarEntryResponse> onLeaveToday) {}
