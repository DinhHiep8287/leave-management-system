package com.peih68.leave.config;

import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already seeded; skipping user initializer");
            return;
        }
        log.info("Seeding demo users (dev profile)");

        Long engId = departmentId("ENG");
        Long salesId = departmentId("SALES");
        Long hrId = departmentId("HR");

        String adminHash = passwordEncoder.encode("Admin@12345");
        String userHash = passwordEncoder.encode("User@12345");

        UserEntity admin = save(UserEntity.builder()
                .employeeCode("E0001").email("admin@demo.local").fullName("Admin Demo")
                .role(Role.ADMIN).departmentId(hrId).managerId(null)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).passwordHash(adminHash).build());

        UserEntity hr = save(UserEntity.builder()
                .employeeCode("E0002").email("hr@demo.local").fullName("HR Demo")
                .role(Role.HR).departmentId(hrId).managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).passwordHash(userHash).build());

        UserEntity engManager = save(UserEntity.builder()
                .employeeCode("E0003").email("eng.manager@demo.local").fullName("Eng Manager")
                .role(Role.MANAGER).departmentId(engId).managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).passwordHash(userHash).build());

        UserEntity salesManager = save(UserEntity.builder()
                .employeeCode("E0004").email("sales.manager@demo.local").fullName("Sales Manager")
                .role(Role.MANAGER).departmentId(salesId).managerId(admin.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).passwordHash(userHash).build());

        // 5 employees
        for (int i = 1; i <= 3; i++) {
            save(UserEntity.builder()
                    .employeeCode(String.format("E%04d", 4 + i))
                    .email("eng.emp" + i + "@demo.local")
                    .fullName("Eng Employee " + i)
                    .role(Role.EMPLOYEE).departmentId(engId).managerId(engManager.getId())
                    .joinDate(LocalDate.of(2025, 1, 1)).isActive(true).passwordHash(userHash).build());
        }
        for (int i = 1; i <= 2; i++) {
            save(UserEntity.builder()
                    .employeeCode(String.format("E%04d", 7 + i))
                    .email("sales.emp" + i + "@demo.local")
                    .fullName("Sales Employee " + i)
                    .role(Role.EMPLOYEE).departmentId(salesId).managerId(salesManager.getId())
                    .joinDate(LocalDate.of(2025, 1, 1)).isActive(true).passwordHash(userHash).build());
        }

        // Backfill department head
        jdbcTemplate.update("UPDATE departments SET head_user_id = ? WHERE code = 'ENG'", engManager.getId());
        jdbcTemplate.update("UPDATE departments SET head_user_id = ? WHERE code = 'SALES'", salesManager.getId());
        jdbcTemplate.update("UPDATE departments SET head_user_id = ? WHERE code = 'HR'", hr.getId());

        log.info("Demo users seeded: {} users", userRepository.count());
        log.info("Login: admin@demo.local / Admin@12345  (others: */@User@12345)");
    }

    private UserEntity save(UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    private Long departmentId(String code) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM departments WHERE code = ?", Long.class, code);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Department not seeded: " + code);
        }
        return ids.getFirst();
    }
}
