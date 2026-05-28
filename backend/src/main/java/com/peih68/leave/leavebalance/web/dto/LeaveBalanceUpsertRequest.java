package com.peih68.leave.leavebalance.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LeaveBalanceUpsertRequest(
        @NotNull Long userId,
        @NotNull Long leaveTypeId,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull
        @DecimalMin(value = "0.0")
        @Digits(integer = 3, fraction = 1)
        BigDecimal totalDays) {}
