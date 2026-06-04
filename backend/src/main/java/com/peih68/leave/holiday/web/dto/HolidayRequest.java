package com.peih68.leave.holiday.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayRequest(
        @NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description) {}
