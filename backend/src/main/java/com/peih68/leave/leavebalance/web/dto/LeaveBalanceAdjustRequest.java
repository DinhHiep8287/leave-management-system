package com.peih68.leave.leavebalance.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** HR/ADMIN tweak of adjusted_days. Delta may be negative. */
public record LeaveBalanceAdjustRequest(
        @NotNull
        @Digits(integer = 3, fraction = 1)
        BigDecimal adjustedDaysDelta,
        @NotBlank @Size(max = 500) String reason) {}
