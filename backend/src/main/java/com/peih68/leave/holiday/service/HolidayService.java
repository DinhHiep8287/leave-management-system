package com.peih68.leave.holiday.service;

import com.peih68.leave.holiday.domain.HolidayEntity;
import com.peih68.leave.holiday.repository.HolidayRepository;
import com.peih68.leave.holiday.web.dto.HolidayResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<HolidayResponse> listByYear(int year) {
        return holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDateAsc(
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream()
                .map(HolidayService::toResponse)
                .toList();
    }

    /** Holiday dates within [from, to] inclusive — reused by leave-day calculation. */
    @Transactional(readOnly = true)
    public Set<LocalDate> holidayDatesBetween(LocalDate from, LocalDate to) {
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(from, to).stream()
                .map(HolidayEntity::getHolidayDate)
                .collect(Collectors.toSet());
    }

    private static HolidayResponse toResponse(HolidayEntity e) {
        return new HolidayResponse(e.getId(), e.getHolidayDate(), e.getName(), e.getDescription());
    }
}
