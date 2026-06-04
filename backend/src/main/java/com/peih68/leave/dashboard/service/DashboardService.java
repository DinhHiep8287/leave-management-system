package com.peih68.leave.dashboard.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.dashboard.web.dto.DashboardSummaryResponse;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceResponse;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.service.LeaveCalendarService;
import com.peih68.leave.leaverequest.web.dto.CalendarEntryResponse;
import com.peih68.leave.user.domain.Role;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates role-aware figures for the home dashboard: how many requests await the
 * caller's approval, who is on leave today within their scope, the caller's own pending
 * requests, and the caller's current-year balances.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeaveRequestRepository requestRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveCalendarService calendarService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UserPrincipal principal) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        List<LeaveBalanceResponse> myBalances = leaveBalanceService.findByUser(principal.getId(), year);

        // Reuse the calendar's scoping; APPROVED-only by default.
        List<CalendarEntryResponse> onLeaveToday =
                calendarService.calendar(principal, today, today, null, null, null, false);

        long pendingApprovalCount = pendingApprovalCount(principal);
        long myPendingCount = requestRepository.countByUserIdAndStatus(principal.getId(), LeaveStatus.PENDING);

        return new DashboardSummaryResponse(
                pendingApprovalCount, onLeaveToday.size(), myPendingCount, myBalances, onLeaveToday);
    }

    private long pendingApprovalCount(UserPrincipal principal) {
        Role role = principal.getRole();
        if (role == Role.ADMIN || role == Role.HR) {
            return requestRepository.countByStatus(LeaveStatus.PENDING);
        }
        if (role == Role.MANAGER) {
            return requestRepository.countByManagerIdAndStatus(principal.getId(), LeaveStatus.PENDING);
        }
        return 0L;
    }
}
