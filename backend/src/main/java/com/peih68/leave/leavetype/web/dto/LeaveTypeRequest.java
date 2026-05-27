package com.peih68.leave.leavetype.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record LeaveTypeRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "code may only contain letters, digits, _ and -")
        String code,
        @NotBlank @Size(min = 1, max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "366.0")
        @Digits(integer = 3, fraction = 1)
        BigDecimal defaultQuotaDays,
        Boolean requiresBalance,
        Boolean isActive) {}
