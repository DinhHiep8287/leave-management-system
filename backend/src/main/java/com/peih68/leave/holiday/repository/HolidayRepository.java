package com.peih68.leave.holiday.repository;

import com.peih68.leave.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate from, LocalDate to);
}
