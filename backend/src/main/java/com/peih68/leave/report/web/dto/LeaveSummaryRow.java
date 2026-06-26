package com.peih68.leave.report.web.dto;

import java.math.BigDecimal;

public record LeaveSummaryRow(
        String period,
        String leaveTypeCode,
        BigDecimal totalDays,
        long requestCount) {}
