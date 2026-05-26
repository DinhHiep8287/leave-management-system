package com.peih68.leave.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired UserRepository repo;
    @Autowired JdbcTemplate jdbc;

    private Long engId;

    @BeforeEach
    void setup() {
        engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
    }

    @Test
    void uniqueEmail() {
        repo.save(newUser("U-A", "a@ex.com", Role.EMPLOYEE, null));
        assertThatThrownBy(() -> repo.saveAndFlush(newUser("U-B", "a@ex.com", Role.EMPLOYEE, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueEmployeeCode() {
        repo.save(newUser("UCODE", "x@ex.com", Role.EMPLOYEE, null));
        assertThatThrownBy(() -> repo.saveAndFlush(newUser("UCODE", "y@ex.com", Role.EMPLOYEE, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEmailIgnoreCase() {
        repo.save(newUser("U-FIND", "Find@Ex.com", Role.EMPLOYEE, null));
        assertThat(repo.findByEmailIgnoreCase("find@ex.com")).isPresent();
        assertThat(repo.findByEmailIgnoreCase("FIND@EX.COM")).isPresent();
    }

    @Test
    void searchFiltersByRoleAndDepartmentAndQ() {
        UserEntity mgr = repo.save(newUser("M-1", "mgr@ex.com", Role.MANAGER, null));
        repo.save(newUser("E-1", "emp1@ex.com", Role.EMPLOYEE, mgr.getId()));
        repo.save(newUser("E-2", "emp2@ex.com", Role.EMPLOYEE, mgr.getId()));

        Page<UserEntity> all = repo.search(null, engId, null, true, PageRequest.of(0, 50));
        assertThat(all.getContent()).extracting(UserEntity::getEmployeeCode)
                .contains("M-1", "E-1", "E-2");

        Page<UserEntity> employeesOnly = repo.search(null, engId, Role.EMPLOYEE, true, PageRequest.of(0, 50));
        assertThat(employeesOnly.getContent()).extracting(UserEntity::getRole)
                .allMatch(r -> r == Role.EMPLOYEE);

        Page<UserEntity> byQ = repo.search("emp1", engId, null, true, PageRequest.of(0, 50));
        assertThat(byQ.getContent()).hasSize(1)
                .first().extracting(UserEntity::getEmail).isEqualTo("emp1@ex.com");
    }

    @Test
    void searchExcludesInactive() {
        UserEntity inactive = newUser("INACTIVE-1", "off@ex.com", Role.EMPLOYEE, null);
        inactive.setIsActive(false);
        repo.save(inactive);

        Page<UserEntity> active = repo.search(null, engId, null, true, PageRequest.of(0, 50));
        assertThat(active.getContent()).extracting(UserEntity::getEmployeeCode).doesNotContain("INACTIVE-1");

        Page<UserEntity> all = repo.search(null, engId, null, false, PageRequest.of(0, 50));
        assertThat(all.getContent()).extracting(UserEntity::getEmployeeCode).contains("INACTIVE-1");
    }

    private UserEntity newUser(String code, String email, Role role, Long managerId) {
        return UserEntity.builder()
                .employeeCode(code)
                .email(email)
                .passwordHash("$2a$12$placeholderplaceholderplaceholderplaceholderplaceholder")
                .fullName("User " + code)
                .role(role)
                .departmentId(engId)
                .managerId(managerId)
                .joinDate(LocalDate.of(2024, 1, 1))
                .isActive(true)
                .build();
    }
}
