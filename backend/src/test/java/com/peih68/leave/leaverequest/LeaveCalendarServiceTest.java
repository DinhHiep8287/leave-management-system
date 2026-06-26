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
    private Long unpaidType;
    private Long annualType;

    private UserEntity admin; // ADMIN
    private UserEntity manager; // ENG, manages e1/e2
    private UserEntity manager2; // ENG, manages e3/s1
    private UserEntity e1; // ENG -> manager
    private UserEntity e2; // ENG -> manager
    private UserEntity e3; // ENG -> manager2
    private UserEntity s1; // SALES -> manager2

    @BeforeEach
    void setup() {
        engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        salesId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'SALES'", Long.class);
        unpaidType = jdbc.queryForObject("SELECT id FROM leave_types WHERE code = 'UNPAID'", Long.class);
        annualType = jdbc.queryForObject("SELECT id FROM leave_types WHERE code = 'ANNUAL'", Long.class);

        admin = save("CAL-ADM", "cal.adm@ex.com", Role.ADMIN, engId, null);
        manager = save("CAL-M", "cal.m@ex.com", Role.MANAGER, engId, admin.getId());
        manager2 = save("CAL-M2", "cal.m2@ex.com", Role.MANAGER, engId, admin.getId());
        e1 = save("CAL-E1", "cal.e1@ex.com", Role.EMPLOYEE, engId, manager.getId());
        e2 = save("CAL-E2", "cal.e2@ex.com", Role.EMPLOYEE, engId, manager.getId());
        e3 = save("CAL-E3", "cal.e3@ex.com", Role.EMPLOYEE, engId, manager2.getId());
        s1 = save("CAL-S1", "cal.s1@ex.com", Role.EMPLOYEE, salesId, manager2.getId());

        leave(e1, unpaidType, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4), LeaveStatus.PENDING);
        leave(e2, unpaidType, LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 5), LeaveStatus.APPROVED);
        leave(e3, annualType, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4), LeaveStatus.APPROVED);
        leave(s1, unpaidType, LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 6), LeaveStatus.APPROVED);
    }

    @Test
    void employeeSeesOwnDepartmentApprovedOnly() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(e1), FROM, TO, null, null, null, false));
        // ENG approved only: e2, e3. e1 is pending (excluded); s1 is in SALES.
        assertThat(ids).containsExactlyInAnyOrder(e2.getId(), e3.getId());
        assertThat(ids).doesNotContain(s1.getId(), e1.getId());
    }

    @Test
    void includePendingShowsPending() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(e1), FROM, TO, null, null, null, true));
        assertThat(ids).contains(e1.getId(), e2.getId(), e3.getId());
        assertThat(ids).doesNotContain(s1.getId());
    }

    @Test
    void managerSeesDepartmentNotOtherDepartments() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(manager), FROM, TO, null, null, null, true));
        assertThat(ids).contains(e1.getId(), e2.getId(), e3.getId());
        assertThat(ids).doesNotContain(s1.getId());
    }

    @Test
    void adminSeesAllDepartments() {
        Set<Long> ids = userIds(service.calendar(UserPrincipal.from(admin), FROM, TO, null, null, null, true));
        assertThat(ids).contains(e1.getId(), e2.getId(), e3.getId(), s1.getId());
    }

    @Test
    void departmentFilterNarrowsToOneDepartment() {
        List<CalendarEntryResponse> entries =
                service.calendar(UserPrincipal.from(admin), FROM, TO, salesId, null, null, true);
        Set<Long> ids = userIds(entries);
        assertThat(ids).containsExactly(s1.getId());
        assertThat(entries)
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.departmentId()).isEqualTo(salesId);
                    assertThat(e.departmentName()).isEqualTo("Sales");
                });
    }

    @Test
    void leaveTypeFilterRestrictsByType() {
        Set<Long> ids =
                userIds(service.calendar(UserPrincipal.from(admin), FROM, TO, null, annualType, null, true));
        assertThat(ids).containsExactly(e3.getId()); // only ANNUAL leave
    }

    @Test
    void userFilterRestrictsToOnePerson() {
        Set<Long> ids =
                userIds(service.calendar(UserPrincipal.from(admin), FROM, TO, null, null, e2.getId(), true));
        assertThat(ids).containsExactly(e2.getId());
    }

    @Test
    void rangeExceedingLimitIsRejected() {
        assertThatThrownBy(
                        () -> service.calendar(UserPrincipal.from(admin), FROM, FROM.plusDays(100), null, null, null, true))
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

    private void leave(UserEntity user, Long typeId, LocalDate start, LocalDate end, LeaveStatus status) {
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(user.getId()).leaveTypeId(typeId).startDate(start).endDate(end)
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("1.0")).reason("x").status(status)
                .managerId(user.getManagerId()).build());
    }
}
