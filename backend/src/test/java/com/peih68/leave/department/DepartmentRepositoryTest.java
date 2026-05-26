package com.peih68.leave.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.department.domain.DepartmentEntity;
import com.peih68.leave.department.repository.DepartmentRepository;
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
class DepartmentRepositoryTest {

    @Autowired DepartmentRepository repo;
    @Autowired JdbcTemplate jdbc;

    @Test
    void codeIsUnique() {
        DepartmentEntity a = repo.save(DepartmentEntity.builder()
                .code("UNIQ-A").name("A").isActive(true).build());
        assertThat(a.getId()).isNotNull();
        assertThatThrownBy(() -> {
            repo.saveAndFlush(DepartmentEntity.builder()
                    .code("UNIQ-A").name("Other").isActive(true).build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByCodeIgnoreCase() {
        repo.save(DepartmentEntity.builder().code("FIND-IT").name("X").isActive(true).build());
        assertThat(repo.findByCodeIgnoreCase("find-it")).isPresent();
        assertThat(repo.findByCodeIgnoreCase("FIND-IT")).isPresent();
        assertThat(repo.findByCodeIgnoreCase("nope")).isEmpty();
    }

    @Test
    void searchFiltersByActiveAndQ() {
        repo.save(DepartmentEntity.builder().code("ACT-A").name("Active alpha").isActive(true).build());
        repo.save(DepartmentEntity.builder().code("ACT-B").name("Active beta").isActive(true).build());
        repo.save(DepartmentEntity.builder().code("DISABLED-X").name("Disabled").isActive(false).build());

        Page<DepartmentEntity> activeAll = repo.search(null, true, PageRequest.of(0, 50));
        assertThat(activeAll.getContent())
                .extracting(DepartmentEntity::getCode)
                .contains("ACT-A", "ACT-B")
                .doesNotContain("DISABLED-X");

        Page<DepartmentEntity> withDisabled = repo.search(null, false, PageRequest.of(0, 50));
        assertThat(withDisabled.getContent())
                .extracting(DepartmentEntity::getCode)
                .contains("ACT-A", "ACT-B", "DISABLED-X");

        Page<DepartmentEntity> qByCode = repo.search("act-a", true, PageRequest.of(0, 50));
        assertThat(qByCode.getContent()).hasSize(1).first()
                .extracting(DepartmentEntity::getCode).isEqualTo("ACT-A");

        Page<DepartmentEntity> qByName = repo.search("beta", true, PageRequest.of(0, 50));
        assertThat(qByName.getContent()).hasSize(1).first()
                .extracting(DepartmentEntity::getCode).isEqualTo("ACT-B");
    }
}
