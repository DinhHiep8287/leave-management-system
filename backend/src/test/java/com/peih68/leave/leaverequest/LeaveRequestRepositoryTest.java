package com.peih68.leave.leaverequest;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaveRequestRepositoryTest {

    @Autowired LeaveRequestRepository repo;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired JdbcTemplate jdbc;

    private Long userId;
    private Long managerId;
    private Long typeId;

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        managerId = userRepository.save(UserEntity.builder()
                .employeeCode("LR-MGR").email("lrmgr@ex.com").passwordHash("x")
                .fullName("LR Mgr").role(Role.MANAGER).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build()).getId();
        userId = userRepository.save(UserEntity.builder()
                .employeeCode("LR-EMP").email("lremp@ex.com").passwordHash("x")
                .fullName("LR Emp").role(Role.EMPLOYEE).departmentId(engId).managerId(managerId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build()).getId();
        typeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("LR-TYPE").name("LR Type").defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(true).isActive(true).build()).getId();
    }

    @Test
    void existsOverlapDetectsActiveRequests() {
        save(LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11), LeaveStatus.PENDING);

        assertThat(repo.existsOverlap(userId, List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2030, 1, 9), LocalDate.of(2030, 1, 12))).isTrue();
        assertThat(repo.existsOverlap(userId, List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 5))).isFalse();
    }

    @Test
    void existsOverlapIgnoresTerminalRequests() {
        save(LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11), LeaveStatus.CANCELLED);
        assertThat(repo.existsOverlap(userId, List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11))).isFalse();
    }

    @Test
    void findByUserAndStartDateWindow() {
        save(LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 8), LeaveStatus.PENDING);
        save(LocalDate.of(2031, 3, 3), LocalDate.of(2031, 3, 4), LeaveStatus.APPROVED);

        assertThat(repo.findByUserIdAndStartDateBetweenOrderByStartDateDesc(
                userId, LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31))).hasSize(2);
        assertThat(repo.findByUserIdAndStartDateBetweenOrderByStartDateDesc(
                userId, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31))).hasSize(1);
    }

    @Test
    void findByManagerAndStatus() {
        save(LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 8), LeaveStatus.PENDING);
        assertThat(repo.findByManagerIdAndStatusOrderByStartDateAsc(
                        managerId, LeaveStatus.PENDING, PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(1);
        assertThat(repo.findByManagerIdAndStatusOrderByStartDateAsc(
                        managerId, LeaveStatus.APPROVED, PageRequest.of(0, 20)).getTotalElements())
                .isZero();
    }

    private void save(LocalDate start, LocalDate end, LeaveStatus status) {
        repo.save(LeaveRequestEntity.builder()
                .userId(userId).leaveTypeId(typeId).managerId(managerId)
                .startDate(start).endDate(end)
                .startHalf(LeaveHalf.FULL_DAY).endHalf(LeaveHalf.FULL_DAY)
                .totalDays(new BigDecimal("2.0")).reason("r").status(status)
                .build());
    }
}
