package com.peih68.leave.leaverequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.service.LeaveCalendarService;
import com.peih68.leave.leaverequest.web.dto.CalendarEntryResponse;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
class LeaveCalendarServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 8, 3);
    private static final LocalDate TO = LocalDate.of(2026, 8, 7);

    @Autowired LeaveCalendarService service;
    @Autowired UserRepository userRepository;
    @Autowired LeaveRequestRepository requestRepository;
    @Autowired JdbcTemplate jdbc;

    private Long engId;
    private Long salesId;
    private Long typeId;

    private UserEntity manager;   // M  (ENG)
    private UserEntity manager2;  // M2 (ENG)
    private UserEntity admin;     // ADMIN
    private UserEntity e1;        // ENG, reports to M
    private UserEntity e2;        // ENG, reports to M
    private UserEntity e3;        // ENG, reports to M2
    private UserEntity s1;        // SALES, reports to M2

    @BeforeEach
    void setup() {
        engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        salesId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'SALES'", Long.class);
        typeId = jdbc.queryForObject("SELECT id FROM leave_types WHERE code = 'UNPAID'", Long.class);

        admin = save("CAL-ADM", "cal.adm@ex.com", Role.ADMIN, engId, null);
        manager = save("CAL-M", "cal.m@ex.com", Role.MANAGER, engId, admin.getId());
        manager2 = save("CAL-M2", "cal.m2@ex.com", Role.MANAGER, engId, admin.getId());
        e1 = save("CAL-E1", "cal.e1@ex.com", Role.EMPLOYEE, engId, manager.getId());
        e2 = save("CAL-E2", "cal.e2@ex.com", Role.EMPLOYEE, engId, manager.getId());
        e3 = save("CAL-E3", "cal.e3@ex.com", Role.EMPLOYEE, engId, manager2.getId());
        s1 = save("CAL-S1", "cal.s1@ex.com", Role.EMPLOYEE, salesId, manager2.getId());

        leave(e1, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4), LeaveStatus.PENDING);
        leave(e2, LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 5), LeaveStatus.APPROVED);
        leave(e3, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4), LeaveStatus.APPROVED);
        leave(s1, LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 6), LeaveStatus.APPROVED);
    }

    @Test
    void managerSeesReportsNotOtherTeams() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(manager), FROM, TO, null));
        assertThat(ids).contains(e1.getId(), e2.getId());
        assertThat(ids).doesNotContain(e3.getId(), s1.getId());
    }

    @Test
    void employeeSeesOnlyOwn() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(e1), FROM, TO, null));
        assertThat(ids).containsExactly(e1.getId());
    }

    @Test
    void includesLeaveOverlappingWindowBoundaryExcludesLeaveBeforeWindow() {
        // Starts before the window but ends on the first day → overlaps.
        leave(e1, LocalDate.of(2026, 7, 30), FROM, LeaveStatus.APPROVED);
        // Entirely before the window → excluded.
        leave(e1, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 24), LeaveStatus.APPROVED);

        List<CalendarEntryResponse> entries = service.calendar(UserPrincipal.from(e1), FROM, TO, null);
        assertThat(entries).anySatisfy(e -> assertThat(e.startDate()).isEqualTo(LocalDate.of(2026, 7, 30)));
        assertThat(entries).noneSatisfy(e -> assertThat(e.startDate()).isEqualTo(LocalDate.of(2026, 7, 20)));
    }

    @Test
    void adminSeesAllTeams() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(admin), FROM, TO, null));
        assertThat(ids).contains(e1.getId(), e2.getId(), e3.getId(), s1.getId());
    }

    @Test
    void departmentFilterNarrowsToOneDepartment() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(admin), FROM, TO, salesId));
        assertThat(ids).containsExactly(s1.getId());
    }

    @Test
    void rangeExceedingLimitIsRejected() {
        assertThatThrownBy(() -> service.calendar(UserPrincipal.from(admin), FROM, FROM.plusDays(100), null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    // --- helpers ---

    private Set<Long> userIds(List<CalendarEntryResponse> entries) {
        return entries.stream().map(CalendarEntryResponse::userId).collect(Collectors.toSet());
    }

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
