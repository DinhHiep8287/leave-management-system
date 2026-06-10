package com.peih68.leave.config;

import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final DemoLeaveSeeder leaveSeeder;

    // Extra staff (Vietnamese names) per department: {fullName, email-local-part}.
    private static final String[][] ENG_STAFF = {
            {"Nguyễn Văn An", "an.nguyen"}, {"Trần Thị Bích", "bich.tran"},
            {"Lê Minh Châu", "chau.le"}, {"Phạm Quốc Dũng", "dung.pham"}};
    private static final String[][] SALES_STAFF = {
            {"Hoàng Thị Em", "em.hoang"}, {"Vũ Đức Phong", "phong.vu"},
            {"Đặng Thu Giang", "giang.dang"}, {"Bùi Xuân Hải", "hai.bui"}};
    private static final String[][] HR_STAFF = {
            {"Đỗ Ngọc Yến", "yen.do"}, {"Ngô Văn Khánh", "khanh.ngo"}};

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

        // Everyone except the admin gets seeded leave history (they all have a manager).
        List<UserEntity> staff = new ArrayList<>(List.of(hr, engManager, salesManager));

        // 5 original employees (referenced by README and the e2e smoke — keep as-is)
        for (int i = 1; i <= 3; i++) {
            staff.add(save(UserEntity.builder()
                    .employeeCode(String.format("E%04d", 4 + i))
                    .email("eng.emp" + i + "@demo.local")
                    .fullName("Eng Employee " + i)
                    .role(Role.EMPLOYEE).departmentId(engId).managerId(engManager.getId())
                    .joinDate(LocalDate.of(2025, 1, 1)).isActive(true).passwordHash(userHash).build()));
        }
        for (int i = 1; i <= 2; i++) {
            staff.add(save(UserEntity.builder()
                    .employeeCode(String.format("E%04d", 7 + i))
                    .email("sales.emp" + i + "@demo.local")
                    .fullName("Sales Employee " + i)
                    .role(Role.EMPLOYEE).departmentId(salesId).managerId(salesManager.getId())
                    .joinDate(LocalDate.of(2025, 1, 1)).isActive(true).passwordHash(userHash).build()));
        }

        // Extra Vietnamese-named staff so dashboards, calendar and reports look natural.
        int code = 10;
        code = addStaff(staff, ENG_STAFF, code, engId, engManager.getId(), userHash);
        code = addStaff(staff, SALES_STAFF, code, salesId, salesManager.getId(), userHash);
        addStaff(staff, HR_STAFF, code, hrId, hr.getId(), userHash);

        // Backfill department head
        jdbcTemplate.update("UPDATE departments SET head_user_id = ? WHERE code = 'ENG'", engManager.getId());
        jdbcTemplate.update("UPDATE departments SET head_user_id = ? WHERE code = 'SALES'", salesManager.getId());
        jdbcTemplate.update("UPDATE departments SET head_user_id = ? WHERE code = 'HR'", hr.getId());

        log.info("Demo users seeded: {} users", userRepository.count());
        log.info("Login: admin@demo.local / Admin@12345  (others: */@User@12345)");

        leaveSeeder.seed(staff, admin);
    }

    private int addStaff(List<UserEntity> out, String[][] people, int nextCode,
            Long departmentId, Long managerId, String passwordHash) {
        for (int i = 0; i < people.length; i++) {
            out.add(save(UserEntity.builder()
                    .employeeCode(String.format("E%04d", nextCode))
                    .email(people[i][1] + "@demo.local")
                    .fullName(people[i][0])
                    .role(Role.EMPLOYEE).departmentId(departmentId).managerId(managerId)
                    .joinDate(LocalDate.of(2024, 3, 1).plusMonths(i * 3L))
                    .isActive(true).passwordHash(passwordHash).build()));
            nextCode++;
        }
        return nextCode;
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
