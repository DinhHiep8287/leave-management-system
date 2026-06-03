package com.peih68.leave.holiday.web.dto;

import java.time.LocalDate;

public record HolidayResponse(
        Long id,
        LocalDate holidayDate,
        String name,
        String description) {}
