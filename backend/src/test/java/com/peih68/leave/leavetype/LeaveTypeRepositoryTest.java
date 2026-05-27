package com.peih68.leave.leavetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaveTypeRepositoryTest {

    @Autowired LeaveTypeRepository repo;

    @Test
    void uniqueCode() {
        repo.save(build("UQ-TYPE", true, true));
        assertThatThrownBy(() -> repo.saveAndFlush(build("UQ-TYPE", true, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByCodeIgnoreCase() {
        repo.save(build("WFH-X", true, true));
        assertThat(repo.findByCodeIgnoreCase("wfh-x")).isPresent();
    }

    @Test
    void activeAndRequiresBalanceFilters() {
        repo.save(build("RB-ACTIVE", true, true));
        repo.save(build("RB-NOBAL", false, true));
        LeaveTypeEntity inactive = build("RB-OFF", true, false);
        repo.save(inactive);

        assertThat(repo.findByIsActiveTrueOrderByCodeAsc())
                .extracting(LeaveTypeEntity::getCode)
                .contains("RB-ACTIVE", "RB-NOBAL")
                .doesNotContain("RB-OFF");

        assertThat(repo.findByIsActiveTrueAndRequiresBalanceTrueOrderByCodeAsc())
                .extracting(LeaveTypeEntity::getCode)
                .contains("RB-ACTIVE")
                .doesNotContain("RB-NOBAL", "RB-OFF");
    }

    private LeaveTypeEntity build(String code, boolean requiresBalance, boolean active) {
        return LeaveTypeEntity.builder()
                .code(code).name("Type " + code)
                .defaultQuotaDays(new BigDecimal("12.0"))
                .requiresBalance(requiresBalance).isActive(active)
                .build();
    }
}
