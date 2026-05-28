package com.peih68.leave.leavebalance;

import static org.assertj.core.api.Assertions.assertThat;

import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceAdjustRequest;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceResponse;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class LeaveBalanceServiceTest {

    @Autowired LeaveBalanceService service;
    @Autowired LeaveBalanceRepository balanceRepository;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager em;

    private Long userId;
    private Long typeId;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM audit_log");
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        userId = userRepository.save(UserEntity.builder()
                .employeeCode("SVC-1").email("svc1@ex.com").passwordHash("x")
                .fullName("Svc User").role(Role.EMPLOYEE).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build()).getId();
        typeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("SVC-TYPE").name("Svc Type").defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(true).isActive(true).build()).getId();
    }

    @Test
    void bulkInitializeIsIdempotent() {
        int first = service.bulkInitializeYear(2030);
        assertThat(first).isGreaterThanOrEqualTo(1);
        int second = service.bulkInitializeYear(2030);
        assertThat(second).isZero();
    }

    @Test
    void adjustUpdatesRemainingAndWritesAudit() {
        service.upsert(new com.peih68.leave.leavebalance.web.dto.LeaveBalanceUpsertRequest(
                userId, typeId, 2031, new BigDecimal("12.0")));
        Long balanceId = balanceRepository.findByUserIdAndLeaveTypeIdAndYear(userId, typeId, 2031)
                .orElseThrow().getId();

        LeaveBalanceResponse resp = service.adjust(
                balanceId, new LeaveBalanceAdjustRequest(new BigDecimal("2.0"), "manual correction"), null);
        assertThat(resp.adjustedDays()).isEqualByComparingTo("2.0");
        assertThat(resp.remainingDays()).isEqualByComparingTo("14.0");

        Long auditCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'LEAVE_BALANCE_ADJUST' AND target_id = ?",
                Long.class, balanceId);
        assertThat(auditCount).isEqualTo(1L);
    }

    @Test
    void upsertOverwritesTotalDaysButKeepsUsedAndAdjusted() {
        service.upsert(new com.peih68.leave.leavebalance.web.dto.LeaveBalanceUpsertRequest(
                userId, typeId, 2032, new BigDecimal("12.0")));
        Long balanceId = balanceRepository.findByUserIdAndLeaveTypeIdAndYear(userId, typeId, 2032)
                .orElseThrow().getId();
        // simulate some usage + adjustment; flush the INSERT first, then evict so
        // the service re-reads the JDBC-updated row instead of a stale cached entity
        em.flush();
        jdbc.update("UPDATE leave_balances SET used_days = 3.0, adjusted_days = 1.0 WHERE id = ?", balanceId);
        em.clear();

        LeaveBalanceResponse resp = service.upsert(
                new com.peih68.leave.leavebalance.web.dto.LeaveBalanceUpsertRequest(
                        userId, typeId, 2032, new BigDecimal("20.0")));
        assertThat(resp.totalDays()).isEqualByComparingTo("20.0");
        assertThat(resp.usedDays()).isEqualByComparingTo("3.0");
        assertThat(resp.adjustedDays()).isEqualByComparingTo("1.0");
        assertThat(resp.remainingDays()).isEqualByComparingTo("18.0");
    }
}
