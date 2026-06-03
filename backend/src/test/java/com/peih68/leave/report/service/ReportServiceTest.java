package com.peih68.leave.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
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
class ReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @Autowired ReportService service;
    @Autowired UserRepository userRepository;
    @Autowired LeaveRequestRepository requestRepository;
    @Autowired LeaveBalanceRepository balanceRepository;
    @Autowired JdbcTemplate jdbc;

    private Long typeId;
    private UserEntity manager;
    private UserEntity employee;

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        typeId = jdbc.queryForObject("SELECT id FROM leave_types WHERE code = 'UNPAID'", Long.class);

        manager = userRepository.save(UserEntity.builder()
                .employeeCode("RPT-M").email("rpt.m@ex.com").passwordHash("x").fullName("Rpt Mgr")
                .role(Role.MANAGER).departmentId(engId).joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        employee = userRepository.save(UserEntity.builder()
                .employeeCode("RPT-E").email("rpt.e@ex.com").passwordHash("x").fullName("Rpt Emp")
                .role(Role.EMPLOYEE).departmentId(engId).managerId(manager.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
    }

    @Test
    void leaveRequestsCsvHasHeaderAndRowWithNames() {
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(employee.getId()).leaveTypeId(typeId)
                .startDate(LocalDate.of(2026, 9, 7)).endDate(LocalDate.of(2026, 9, 9))
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("3.0")).reason("trip").status(LeaveStatus.APPROVED)
                .managerId(manager.getId()).build());

        String csv = service.leaveRequestsCsv(FROM, TO, null);
        String[] lines = csv.split("\r\n");
        assertThat(lines[0]).contains("employeeCode", "userFullName", "leaveTypeCode", "status");
        assertThat(csv).contains("RPT-E", "Rpt Emp", "UNPAID", "APPROVED", "Rpt Mgr");
        // header + exactly one data row for this employee in September.
        assertThat(csv).contains("3.0");
    }

    @Test
    void leaveRequestsCsvStatusFilterExcludesOtherStatuses() {
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(employee.getId()).leaveTypeId(typeId)
                .startDate(LocalDate.of(2026, 9, 7)).endDate(LocalDate.of(2026, 9, 7))
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("1.0")).reason("pending one").status(LeaveStatus.PENDING)
                .managerId(manager.getId()).build());

        String approvedOnly = service.leaveRequestsCsv(FROM, TO, LeaveStatus.APPROVED);
        assertThat(approvedOnly).doesNotContain("pending one");
    }

    @Test
    void leaveBalancesCsvHasHeaderAndComputedRemaining() {
        balanceRepository.save(LeaveBalanceEntity.builder()
                .userId(employee.getId()).leaveTypeId(typeId).year(2026)
                .totalDays(new BigDecimal("12.0")).usedDays(new BigDecimal("2.0"))
                .adjustedDays(new BigDecimal("1.0")).build());

        String csv = service.leaveBalancesCsv(2026);
        String[] lines = csv.split("\r\n");
        assertThat(lines[0]).contains("userFullName", "remainingDays");
        assertThat(csv).contains("Rpt Emp", "RPT-E", "UNPAID");
        assertThat(csv).contains("11.0"); // 12 + 1 - 2
    }
}
