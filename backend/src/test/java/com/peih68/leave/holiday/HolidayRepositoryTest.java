package com.peih68.leave.holiday;

import static org.assertj.core.api.Assertions.assertThat;

import com.peih68.leave.holiday.domain.HolidayEntity;
import com.peih68.leave.holiday.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HolidayRepositoryTest {

    @Autowired HolidayRepository repo;

    @Test
    void findsSeeded2026HolidaysOrderedAscending() {
        List<HolidayEntity> holidays = repo.findByHolidayDateBetweenOrderByHolidayDateAsc(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(holidays).hasSizeGreaterThanOrEqualTo(10);
        assertThat(holidays.get(0).getHolidayDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(holidays).isSortedAccordingTo(
                (a, b) -> a.getHolidayDate().compareTo(b.getHolidayDate()));
    }

    @Test
    void rangeOutsideSeedDataIsEmpty() {
        assertThat(repo.findByHolidayDateBetweenOrderByHolidayDateAsc(
                        LocalDate.of(2099, 1, 1), LocalDate.of(2099, 12, 31)))
                .isEmpty();
    }
}
