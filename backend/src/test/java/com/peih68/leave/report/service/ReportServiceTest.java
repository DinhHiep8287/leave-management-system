package com.peih68.leave.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
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
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired JdbcTemplate jdbc;

    private Long typeId;
    private Long engId;
    private Long hrId;
    private UserEntity manager;
    private UserEntity employee;

    @BeforeEach
    void setup() {
        engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        hrId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'HR'", Long.class);
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

        String csv = service.leaveRequestsCsv(FROM, TO, null, null);
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

        String approvedOnly = service.leaveRequestsCsv(FROM, TO, LeaveStatus.APPROVED, null);
        assertThat(approvedOnly).doesNotContain("pending one");
    }

    @Test
    void leaveSummaryCsvAggregatesApprovedDaysByMonth() {
        // Dedicated type so leftover data in the shared test DB can't skew the sum.
        Long sumType = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("RPT-SUM").name("Sum").defaultQuotaDays(new BigDecimal("0.0"))
                .requiresBalance(false).isActive(true).build()).getId();
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(employee.getId()).leaveTypeId(sumType)
                .startDate(LocalDate.of(2026, 9, 7)).endDate(LocalDate.of(2026, 9, 9))
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("3.0")).reason("a").status(LeaveStatus.APPROVED)
                .managerId(manager.getId()).build());
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(employee.getId()).leaveTypeId(sumType)
                .startDate(LocalDate.of(2026, 9, 21)).endDate(LocalDate.of(2026, 9, 22))
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("2.0")).reason("b").status(LeaveStatus.APPROVED)
                .managerId(manager.getId()).build());

        String csv = service.leaveSummaryCsv(2026, "month");
        assertThat(csv.split("\r\n")[0]).contains("month", "leaveTypeCode", "totalDays");
        // September (09) RPT-SUM total = 3 + 2 = 5.0 on one row.
        assertThat(csv).contains("09,RPT-SUM,5.0");
    }

    @Test
    void leaveSummaryCanBeFilteredByDepartment() {
        Long sumType = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("RPT-DEPT").name("Dept Sum").defaultQuotaDays(new BigDecimal("0.0"))
                .requiresBalance(false).isActive(true).build()).getId();
        UserEntity otherDeptUser = userRepository.save(UserEntity.builder()
                .employeeCode("RPT-HR").email("rpt.hr@ex.com").passwordHash("x").fullName("Rpt HR")
                .role(Role.EMPLOYEE).departmentId(hrId).joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(employee.getId()).leaveTypeId(sumType)
                .startDate(LocalDate.of(2026, 9, 7)).endDate(LocalDate.of(2026, 9, 8))
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("2.0")).reason("eng").status(LeaveStatus.APPROVED)
                .managerId(manager.getId()).build());
        requestRepository.save(LeaveRequestEntity.builder()
                .userId(otherDeptUser.getId()).leaveTypeId(sumType)
                .startDate(LocalDate.of(2026, 9, 9)).endDate(LocalDate.of(2026, 9, 11))
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("3.0")).reason("hr").status(LeaveStatus.APPROVED)
                .managerId(manager.getId()).build());

        var rows = service.leaveSummary(2026, "month", engId);

        assertThat(rows)
                .anySatisfy(row -> {
                    assertThat(row.period()).isEqualTo("09");
                    assertThat(row.leaveTypeCode()).isEqualTo("RPT-DEPT");
                    assertThat(row.totalDays()).isEqualByComparingTo("2.0");
                    assertThat(row.requestCount()).isEqualTo(1);
                });
        assertThat(service.leaveSummaryCsv(2026, "month", engId)).contains("09,RPT-DEPT,2.0,1");
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

    @Test
    void leaveBalancesCsvCanBeFilteredByDepartment() {
        UserEntity otherDeptUser = userRepository.save(UserEntity.builder()
                .employeeCode("RPT-HR2").email("rpt.hr2@ex.com").passwordHash("x").fullName("Rpt HR2")
                .role(Role.EMPLOYEE).departmentId(hrId).joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        balanceRepository.save(LeaveBalanceEntity.builder()
                .userId(employee.getId()).leaveTypeId(typeId).year(2026)
                .totalDays(new BigDecimal("12.0")).usedDays(new BigDecimal("2.0"))
                .adjustedDays(new BigDecimal("0.0")).build());
        balanceRepository.save(LeaveBalanceEntity.builder()
                .userId(otherDeptUser.getId()).leaveTypeId(typeId).year(2026)
                .totalDays(new BigDecimal("12.0")).usedDays(new BigDecimal("5.0"))
                .adjustedDays(new BigDecimal("0.0")).build());

        String csv = service.leaveBalancesCsv(2026, engId);

        assertThat(csv).contains("Rpt Emp");
        assertThat(csv).doesNotContain("Rpt HR2");
    }
}
