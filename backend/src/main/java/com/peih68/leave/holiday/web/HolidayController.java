package com.peih68.leave.holiday.web;

import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.holiday.web.dto.HolidayRequest;
import com.peih68.leave.holiday.web.dto.HolidayResponse;
import jakarta.validation.Valid;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /** Anyone authenticated can read holidays for a year. */
    @GetMapping
    public ApiResponse<List<HolidayResponse>> list(
            @RequestParam(name = "year", required = false) Integer year) {
        int target = year != null ? year : Year.now().getValue();
        return ApiResponse.ok(holidayService.listByYear(target));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HolidayResponse> create(@Valid @RequestBody HolidayRequest req) {
        return ApiResponse.ok(holidayService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<HolidayResponse> update(
            @PathVariable Long id, @Valid @RequestBody HolidayRequest req) {
        return ApiResponse.ok(holidayService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        holidayService.delete(id);
    }
}
