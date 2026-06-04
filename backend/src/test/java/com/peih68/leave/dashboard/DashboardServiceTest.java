package com.peih68.leave.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.dashboard.service.DashboardService;
import com.peih68.leave.dashboard.web.dto.AdminSummaryResponse;
import com.peih68.leave.dashboard.web.dto.DashboardSummaryResponse;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired DashboardService service;
    @Autowired UserRepository userRepository;
    @Autowired LeaveRequestRepository requestRepository;
    @Autowired JdbcTemplate jdbc;

    private Long typeId;
    private UserEntity admin;
    private UserEntity manager;
    private UserEntity manager2;
    private UserEntity e1;   // reports to manager
    private UserEntity e3;   // reports to manager2

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        typeId = jdbc.queryForObject("SELECT id FROM leave_types WHERE code = 'UNPAID'", Long.class);

        admin = save("DSH-ADM", "dsh.adm@ex.com", Role.ADMIN, engId, null);
        manager = save("DSH-M", "dsh.m@ex.com", Role.MANAGER, engId, admin.getId());
        manager2 = save("DSH-M2", "dsh.m2@ex.com", Role.MANAGER, engId, admin.getId());
        e1 = save("DSH-E1", "dsh.e1@ex.com", Role.EMPLOYEE, engId, manager.getId());
        e3 = save("DSH-E3", "dsh.e3@ex.com", Role.EMPLOYEE, engId, manager2.getId());

        // e1: one PENDING (future) + one APPROVED covering today.
        leave(e1, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11), LeaveStatus.PENDING);
        leave(e1, TODAY.minusDays(1), TODAY.plusDays(1), LeaveStatus.APPROVED);
        // e3: one PENDING under a different manager.
        leave(e3, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11), LeaveStatus.PENDING);
    }

    @Test
    void managerSummaryCountsTeamPendingAndOnLeaveToday() {
        DashboardSummaryResponse s = service.summary(UserPrincipal.from(manager));
        assertThat(s.pendingApprovalCount()).isEqualTo(1L);   // only e1's pending
        assertThat(s.onLeaveTodayCount()).isEqualTo(1);       // e1 approved covers today
        assertThat(s.myPendingCount()).isEqualTo(0L);
        assertThat(s.myBalances()).isNotNull();
    }

    @Test
    void employeeSummaryCountsOwnPending() {
        DashboardSummaryResponse s = service.summary(UserPrincipal.from(e1));
        assertThat(s.pendingApprovalCount()).isEqualTo(0L);   // not an approver
        assertThat(s.myPendingCount()).isEqualTo(1L);
        assertThat(s.onLeaveTodayCount()).isEqualTo(1);       // own approved covers today
    }

    @Test
    void adminCountsPendingAcrossAllTeams() {
        DashboardSummaryResponse s = service.summary(UserPrincipal.from(admin));
        // Sees both e1's and e3's pending (cross-manager); at least the two seeded here.
        assertThat(s.pendingApprovalCount()).isGreaterThanOrEqualTo(2L);
    }

    @Test
    void adminSummaryAggregatesOrgWide() {
        AdminSummaryResponse s = service.adminSummary();
        assertThat(s.totalActiveEmployees()).isGreaterThanOrEqualTo(5L);
        assertThat(s.pendingCount()).isGreaterThanOrEqualTo(2L); // e1 + e3 pending
        assertThat(s.approvedCount()).isGreaterThanOrEqualTo(1L); // e1 approved covers today
        assertThat(s.topDepartmentsThisMonth()).isNotEmpty();
    }

    // --- helpers ---

    private UserEntity save(String code, String email, Role role, Long deptId, Long managerId) {
        return userRepository.save(UserEntity.builder()
                .employeeCode(code).email(email).passwordHash("x").fullName(code)
                .role(role).departmentId(deptId).managerId(managerId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
    }

    private void leave(UserEntity user, LocalDate start, LocalDate end, LeaveStatus status) {
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(user.getId()).leaveTypeId(typeId).startDate(start).endDate(end)
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("1.0")).reason("x").status(status)
                .managerId(user.getManagerId()).build());
    }
}
