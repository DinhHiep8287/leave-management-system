package com.peih68.leave.holiday.service;

import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.holiday.domain.HolidayEntity;
import com.peih68.leave.holiday.repository.HolidayRepository;
import com.peih68.leave.holiday.web.dto.HolidayRequest;
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

    /** Admin/HR adds a holiday. The date must be unique. */
    @Transactional
    public HolidayResponse create(HolidayRequest req) {
        if (holidayRepository.existsByHolidayDate(req.holidayDate())) {
            throw new ApiException(ErrorCode.CONFLICT, "a holiday already exists on " + req.holidayDate());
        }
        HolidayEntity saved = holidayRepository.save(HolidayEntity.builder()
                .holidayDate(req.holidayDate())
                .name(req.name())
                .description(req.description())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public HolidayResponse update(Long id, HolidayRequest req) {
        HolidayEntity entity = requireHoliday(id);
        if (!entity.getHolidayDate().equals(req.holidayDate())
                && holidayRepository.existsByHolidayDate(req.holidayDate())) {
            throw new ApiException(ErrorCode.CONFLICT, "a holiday already exists on " + req.holidayDate());
        }
        entity.setHolidayDate(req.holidayDate());
        entity.setName(req.name());
        entity.setDescription(req.description());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        holidayRepository.delete(requireHoliday(id));
    }

    private HolidayEntity requireHoliday(Long id) {
        return holidayRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Holiday not found: " + id));
    }

    private static HolidayResponse toResponse(HolidayEntity e) {
        return new HolidayResponse(e.getId(), e.getHolidayDate(), e.getName(), e.getDescription());
    }
}
