package com.peih68.leave.leavebalance;

import static org.assertj.core.api.Assertions.assertThat;

import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
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
class CarryOverServiceTest {

    private static final int FROM = 2031; // far-future years to avoid colliding with seeded data
    private static final int TO = 2032;

    @Autowired LeaveBalanceService service;
    @Autowired LeaveBalanceRepository balanceRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbc;

    private UserEntity user;
    private Long annualId;   // requiresBalance = true, quota 12
    private Long unpaidId;   // requiresBalance = false

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        user = userRepository.save(UserEntity.builder()
                .employeeCode("CO-EMP").email("coemp@ex.com").passwordHash("x")
                .fullName("CO Emp").role(Role.EMPLOYEE).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        annualId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("CO-ANNUAL").name("Annual").defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(true).isActive(true).build()).getId();
        unpaidId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("CO-UNPAID").name("Unpaid").defaultQuotaDays(new BigDecimal("0.0"))
                .requiresBalance(false).isActive(true).build()).getId();
    }

    private LeaveBalanceEntity balance(Long typeId, int year, String total, String used) {
        return balanceRepository.save(LeaveBalanceEntity.builder()
                .userId(user.getId()).leaveTypeId(typeId).year(year)
                .totalDays(new BigDecimal(total)).usedDays(new BigDecimal(used))
                .adjustedDays(BigDecimal.ZERO).build());
    }

    @Test
    void carriesRemainingCappedAndCreatesTargetRow() {
        balance(annualId, FROM, "12.0", "4.0"); // remaining 8 > cap 5

        int carried = service.carryOverYear(FROM, new BigDecimal("5.0"), null);

        assertThat(carried).isEqualTo(1);
        LeaveBalanceEntity target = balanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(user.getId(), annualId, TO).orElseThrow();
        assertThat(target.getCarriedOverDays()).isEqualByComparingTo("5.0");
        assertThat(target.getTotalDays()).isEqualByComparingTo("12.0"); // default quota
        assertThat(target.remaining()).isEqualByComparingTo("17.0");
    }

    @Test
    void carriesLessThanCapWhenRemainingIsSmaller() {
        balance(annualId, FROM, "12.0", "10.5"); // remaining 1.5 < cap 5
        service.carryOverYear(FROM, new BigDecimal("5.0"), null);

        LeaveBalanceEntity target = balanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(user.getId(), annualId, TO).orElseThrow();
        assertThat(target.getCarriedOverDays()).isEqualByComparingTo("1.5");
    }

    @Test
    void isIdempotentAcrossReruns() {
        balance(annualId, FROM, "12.0", "0.0");
        assertThat(service.carryOverYear(FROM, new BigDecimal("5.0"), null)).isEqualTo(1);
        assertThat(service.carryOverYear(FROM, new BigDecimal("5.0"), null)).isZero();

        LeaveBalanceEntity target = balanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(user.getId(), annualId, TO).orElseThrow();
        assertThat(target.getCarriedOverDays()).isEqualByComparingTo("5.0"); // not doubled
    }

    @Test
    void skipsNonBalanceTypesAndZeroRemaining() {
        balance(unpaidId, FROM, "0.0", "0.0");   // requiresBalance=false
        balance(annualId, FROM, "12.0", "12.0"); // remaining 0

        assertThat(service.carryOverYear(FROM, new BigDecimal("5.0"), null)).isZero();
        assertThat(balanceRepository.findByUserIdAndLeaveTypeIdAndYear(user.getId(), unpaidId, TO)).isEmpty();
    }
}
