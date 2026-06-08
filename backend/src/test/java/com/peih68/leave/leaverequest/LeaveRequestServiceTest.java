package com.peih68.leave.leaverequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.ApprovalActionRepository;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestResponse;
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
class LeaveRequestServiceTest {

    // A future Mon–Fri work week with no seeded holidays → 5 working days.
    private static final LocalDate MON = LocalDate.of(2026, 7, 6);
    private static final LocalDate FRI = LocalDate.of(2026, 7, 10);

    @Autowired LeaveRequestService service;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired ApprovalActionRepository approvalActionRepository;
    @Autowired JdbcTemplate jdbc;

    private UserEntity employee;
    private Long unpaidTypeId;   // requiresBalance = false
    private Long paidTypeId;     // requiresBalance = true

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        Long mgrId = userRepository.save(UserEntity.builder()
                .employeeCode("LRS-MGR").email("lrsmgr@ex.com").passwordHash("x")
                .fullName("LRS Mgr").role(Role.MANAGER).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build()).getId();
        employee = userRepository.save(UserEntity.builder()
                .employeeCode("LRS-EMP").email("lrsemp@ex.com").passwordHash("x")
                .fullName("LRS Emp").role(Role.EMPLOYEE).departmentId(engId).managerId(mgrId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        unpaidTypeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("LRS-UNPAID").name("Unpaid").defaultQuotaDays(new BigDecimal("0.0"))
                .requiresBalance(false).isActive(true).build()).getId();
        paidTypeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("LRS-PAID").name("Paid").defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(true).isActive(true).build()).getId();
    }

    private LeaveRequestCreateRequest req(Long typeId, LocalDate start, LocalDate end) {
        return new LeaveRequestCreateRequest(
                typeId, start, end, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, "reason");
    }

    @Test
    void submitComputesWorkingDaysSetsManagerAndRecordsCreated() {
        LeaveRequestResponse resp =
                service.submit(req(unpaidTypeId, MON, FRI), UserPrincipal.from(employee));

        assertThat(resp.totalDays()).isEqualByComparingTo("5.0");
        assertThat(resp.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(resp.managerId()).isEqualTo(employee.getManagerId());

        assertThat(approvalActionRepository.findByLeaveRequestIdOrderByCreatedAtAsc(resp.id()))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.getAction()).isEqualTo(ApprovalAction.CREATED);
                    assertThat(a.getNewStatus()).isEqualTo(LeaveStatus.PENDING);
                    assertThat(a.getActorId()).isEqualTo(employee.getId());
                });
    }

    @Test
    void submitWithoutManagerIsRejected() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        UserEntity orphan = userRepository.save(UserEntity.builder()
                .employeeCode("LRS-ORPH").email("orph@ex.com").passwordHash("x")
                .fullName("Orphan").role(Role.EMPLOYEE).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());

        assertThatThrownBy(() -> service.submit(req(unpaidTypeId, MON, FRI), UserPrincipal.from(orphan)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void submitOverlappingIsConflict() {
        service.submit(req(unpaidTypeId, MON, FRI), UserPrincipal.from(employee));
        assertThatThrownBy(() -> service.submit(
                        req(unpaidTypeId, LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 9)),
                        UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void submitWithInsufficientBalanceIsRejected() {
        // paid type requires balance but no balance row exists → remaining 0 < 5.0
        assertThatThrownBy(() -> service.submit(req(paidTypeId, MON, FRI), UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    void submitWithPastStartDateIsRejected() {
        // Requirements §5.3: start_date must not be in the past.
        LocalDate past = LocalDate.now().minusDays(3);
        assertThatThrownBy(() -> service.submit(req(unpaidTypeId, past, past), UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void submitEntirelyWeekendIsRejected() {
        // Sat 2026-07-11 .. Sun 2026-07-12 → 0 working days
        assertThatThrownBy(() -> service.submit(
                        req(unpaidTypeId, LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 12)),
                        UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
