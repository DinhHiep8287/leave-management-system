package com.peih68.leave.holiday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.holiday.repository.HolidayRepository;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.holiday.web.dto.HolidayRequest;
import com.peih68.leave.holiday.web.dto.HolidayResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HolidayServiceTest {

    // Use 2027 dates to avoid collisions with the 2026 holidays seeded in V2.
    private static final LocalDate D1 = LocalDate.of(2027, 3, 15);
    private static final LocalDate D2 = LocalDate.of(2027, 3, 16);

    @Autowired HolidayService service;
    @Autowired HolidayRepository repo;

    @Test
    void createAddsHoliday() {
        HolidayResponse r = service.create(new HolidayRequest(D1, "Ngày test", "mô tả"));
        assertThat(r.id()).isNotNull();
        assertThat(repo.existsByHolidayDate(D1)).isTrue();
    }

    @Test
    void createDuplicateDateIsConflict() {
        service.create(new HolidayRequest(D1, "Ngày test", null));
        assertThatThrownBy(() -> service.create(new HolidayRequest(D1, "Trùng", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void updateChangesFields() {
        Long id = service.create(new HolidayRequest(D1, "Cũ", null)).id();
        HolidayResponse r = service.update(id, new HolidayRequest(D2, "Mới", "x"));
        assertThat(r.holidayDate()).isEqualTo(D2);
        assertThat(r.name()).isEqualTo("Mới");
    }

    @Test
    void updateToExistingDateIsConflict() {
        service.create(new HolidayRequest(D1, "A", null));
        Long id2 = service.create(new HolidayRequest(D2, "B", null)).id();
        assertThatThrownBy(() -> service.update(id2, new HolidayRequest(D1, "B", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void deleteRemoves() {
        Long id = service.create(new HolidayRequest(D1, "A", null)).id();
        service.delete(id);
        assertThat(repo.existsByHolidayDate(D1)).isFalse();
    }

    @Test
    void updateMissingIsNotFound() {
        assertThatThrownBy(() -> service.update(999_999L, new HolidayRequest(D1, "A", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
