package com.peih68.leave.leaverequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.ApprovalActionRepository;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.leaverequest.web.dto.ApprovalDecisionRequest;
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
class LeaveRequestApprovalServiceTest {

    private static final LocalDate MON = LocalDate.of(2026, 7, 6);
    private static final LocalDate FRI = LocalDate.of(2026, 7, 10); // 5 working days

    @Autowired LeaveRequestService service;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired LeaveBalanceRepository balanceRepository;
    @Autowired ApprovalActionRepository approvalActionRepository;
    @Autowired JdbcTemplate jdbc;

    private UserEntity employee;
    private UserPrincipal managerPrincipal;
    private Long typeId;

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        UserEntity manager = userRepository.save(UserEntity.builder()
                .employeeCode("AP-MGR").email("apmgr@ex.com").passwordHash("x")
                .fullName("AP Mgr").role(Role.MANAGER).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        managerPrincipal = UserPrincipal.from(manager);
        employee = userRepository.save(UserEntity.builder()
                .employeeCode("AP-EMP").email("apemp@ex.com").passwordHash("x")
                .fullName("AP Emp").role(Role.EMPLOYEE).departmentId(engId).managerId(manager.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        typeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("AP-TYPE").name("AP Type").defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(true).isActive(true).build()).getId();
        balanceRepository.save(LeaveBalanceEntity.builder()
                .userId(employee.getId()).leaveTypeId(typeId).year(2026)
                .totalDays(new BigDecimal("12.0")).usedDays(BigDecimal.ZERO).adjustedDays(BigDecimal.ZERO)
                .build());
    }

    private Long submit() {
        LeaveRequestResponse r = service.submit(
                new LeaveRequestCreateRequest(typeId, MON, FRI, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, "trip"),
                UserPrincipal.from(employee));
        return r.id();
    }

    private BigDecimal usedDays() {
        return balanceRepository.findByUserIdAndLeaveTypeIdAndYear(employee.getId(), typeId, 2026)
                .orElseThrow().getUsedDays();
    }

    private long auditCount(Long requestId, String action) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND target_type = 'leave_request' AND target_id = ?",
                Long.class, action, requestId);
    }

    @Test
    void approveConsumesBalanceAndRecordsHistoryAndAudit() {
        Long id = submit();
        LeaveRequestResponse resp = service.approve(id, new ApprovalDecisionRequest("ok"), managerPrincipal);

        assertThat(resp.status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(usedDays()).isEqualByComparingTo("5.0");
        assertThat(approvalActionRepository.findByLeaveRequestIdOrderByCreatedAtAsc(id)).hasSize(2); // CREATED + APPROVED
        assertThat(auditCount(id, "LEAVE_REQUEST_APPROVED")).isEqualTo(1L);
    }

    @Test
    void rejectDoesNotTouchBalance() {
        Long id = submit();
        LeaveRequestResponse resp = service.reject(id, new ApprovalDecisionRequest("not now"), managerPrincipal);

        assertThat(resp.status()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(usedDays()).isEqualByComparingTo("0.0");
        assertThat(auditCount(id, "LEAVE_REQUEST_REJECTED")).isEqualTo(1L);
    }

    @Test
    void rejectRequiresComment() {
        Long id = submit();
        assertThatThrownBy(() -> service.reject(id, new ApprovalDecisionRequest("  "), managerPrincipal))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void cancelApprovedRestoresBalance() {
        Long id = submit();
        service.approve(id, new ApprovalDecisionRequest("ok"), managerPrincipal);
        assertThat(usedDays()).isEqualByComparingTo("5.0");

        LeaveRequestResponse resp = service.cancel(id, new ApprovalDecisionRequest("changed plans"), managerPrincipal);
        assertThat(resp.status()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(usedDays()).isEqualByComparingTo("0.0");
    }

    @Test
    void requesterCannotCancelApproved() {
        Long id = submit();
        service.approve(id, new ApprovalDecisionRequest("ok"), managerPrincipal);

        assertThatThrownBy(() -> service.cancel(id, null, UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void requesterCanCancelOwnPending() {
        Long id = submit();
        LeaveRequestResponse resp = service.cancel(id, null, UserPrincipal.from(employee));
        assertThat(resp.status()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(usedDays()).isEqualByComparingTo("0.0");
    }

    @Test
    void approvingNonPendingIsConflict() {
        Long id = submit();
        service.reject(id, new ApprovalDecisionRequest("no"), managerPrincipal);
        assertThatThrownBy(() -> service.approve(id, new ApprovalDecisionRequest("ok"), managerPrincipal))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void approveWithInsufficientBalanceIsRejected() {
        Long id = submit(); // soft check passed (remaining 12 >= 5)
        // simulate balance drained by other approved leave before this approval
        LeaveBalanceEntity bal = balanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(employee.getId(), typeId, 2026).orElseThrow();
        bal.setUsedDays(new BigDecimal("12.0"));
        balanceRepository.saveAndFlush(bal);

        assertThatThrownBy(() -> service.approve(id, new ApprovalDecisionRequest("ok"), managerPrincipal))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }
}
