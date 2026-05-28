package com.peih68.leave.leavebalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaveBalanceRepositoryTest {

    @Autowired LeaveBalanceRepository repo;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired JdbcTemplate jdbc;

    private Long userId;
    private Long typeId;

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        userId = userRepository.save(UserEntity.builder()
                .employeeCode("BAL-1").email("bal1@ex.com").passwordHash("x")
                .fullName("Bal User").role(Role.EMPLOYEE).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build()).getId();
        typeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("BAL-TYPE").name("Bal Type").defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(true).isActive(true).build()).getId();
    }

    @Test
    void uniqueUserTypeYear() {
        repo.save(row(2026, "12.0"));
        assertThatThrownBy(() -> repo.saveAndFlush(row(2026, "10.0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentYearAllowed() {
        repo.save(row(2026, "12.0"));
        repo.saveAndFlush(row(2027, "14.0"));
        assertThat(repo.findByUserIdAndLeaveTypeIdAndYear(userId, typeId, 2027)).isPresent();
    }

    @Test
    void findByUserAndYear() {
        repo.save(row(2026, "12.0"));
        assertThat(repo.findByUserIdAndYearOrderByLeaveTypeId(userId, 2026)).hasSize(1);
        assertThat(repo.findByUserIdAndYearOrderByLeaveTypeId(userId, 2099)).isEmpty();
    }

    @Test
    void remainingComputation() {
        LeaveBalanceEntity e = LeaveBalanceEntity.builder()
                .userId(userId).leaveTypeId(typeId).year(2026)
                .totalDays(new BigDecimal("12.0"))
                .usedDays(new BigDecimal("3.5"))
                .adjustedDays(new BigDecimal("2.0"))
                .build();
        assertThat(e.remaining()).isEqualByComparingTo("10.5");
    }

    private LeaveBalanceEntity row(int year, String total) {
        return LeaveBalanceEntity.builder()
                .userId(userId).leaveTypeId(typeId).year(year)
                .totalDays(new BigDecimal(total))
                .usedDays(BigDecimal.ZERO).adjustedDays(BigDecimal.ZERO)
                .build();
    }
}
