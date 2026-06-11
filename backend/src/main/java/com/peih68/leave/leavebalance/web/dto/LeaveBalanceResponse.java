package com.peih68.leave.leavebalance.web.dto;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
        Long id,
        Long userId,
        String userFullName,
        Long leaveTypeId,
        String leaveTypeCode,
        int year,
        BigDecimal totalDays,
        BigDecimal usedDays,
        BigDecimal adjustedDays,
        BigDecimal carriedOverDays,
        BigDecimal remainingDays) {}
