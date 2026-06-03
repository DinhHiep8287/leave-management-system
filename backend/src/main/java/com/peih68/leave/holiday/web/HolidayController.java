package com.peih68.leave.holiday.web;

import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.holiday.web.dto.HolidayResponse;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    public ApiResponse<List<HolidayResponse>> list(
            @RequestParam(name = "year", required = false) Integer year) {
        int target = year != null ? year : Year.now().getValue();
        return ApiResponse.ok(holidayService.listByYear(target));
    }
}
