package com.peih68.leave.dashboard.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.dashboard.web.dto.AdminSummaryResponse;
import com.peih68.leave.dashboard.web.dto.AdminSummaryResponse.DepartmentLeaveCount;
import com.peih68.leave.dashboard.web.dto.DashboardSummaryResponse;
import com.peih68.leave.department.domain.DepartmentEntity;
import com.peih68.leave.department.repository.DepartmentRepository;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceResponse;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.service.LeaveCalendarService;
import com.peih68.leave.leaverequest.web.dto.CalendarEntryResponse;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UserPrincipal principal) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        List<LeaveBalanceResponse> myBalances = leaveBalanceService.findByUser(principal.getId(), year);

        // Reuse the calendar's scoping; APPROVED-only by default.
        List<CalendarEntryResponse> onLeaveToday =
                calendarService.calendar(principal, today, today, null, null, null, false);

        // Distinct people on leave this ISO week (Mon..Sun) within the caller's scope (§10.2).
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        long onLeaveThisWeek = calendarService.calendar(principal, weekStart, weekEnd, null, null, null, false)
                .stream().map(CalendarEntryResponse::userId).distinct().count();

        long pendingApprovalCount = pendingApprovalCount(principal);
        long myPendingCount = requestRepository.countByUserIdAndStatus(principal.getId(), LeaveStatus.PENDING);

        return new DashboardSummaryResponse(
                pendingApprovalCount, onLeaveToday.size(), (int) onLeaveThisWeek,
                myPendingCount, myBalances, onLeaveToday);
    }

    /** HR/ADMIN-wide figures (REQUIREMENTS §10.3). */
    @Transactional(readOnly = true)
    public AdminSummaryResponse adminSummary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        List<DepartmentLeaveCount> topDepts = requestRepository
                .countByDepartmentForStatusInRange(LeaveStatus.APPROVED, monthStart, monthEnd).stream()
                .limit(5)
                .map(row -> {
                    Long deptId = (Long) row[0];
                    long count = ((Number) row[1]).longValue();
                    String name = departmentRepository.findById(deptId)
                            .map(DepartmentEntity::getName).orElse(null);
                    return new DepartmentLeaveCount(deptId, name, count);
                })
                .toList();

        return new AdminSummaryResponse(
                userRepository.countByIsActiveTrue(),
                requestRepository.countByStatus(LeaveStatus.PENDING),
                requestRepository.countByStatus(LeaveStatus.APPROVED),
                requestRepository.countByStatus(LeaveStatus.REJECTED),
                requestRepository.countByStatus(LeaveStatus.CANCELLED),
                topDepts);
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
