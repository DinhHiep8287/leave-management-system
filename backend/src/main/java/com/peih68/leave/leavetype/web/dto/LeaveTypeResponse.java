package com.peih68.leave.leavetype.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LeaveTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal defaultQuotaDays,
        boolean requiresBalance,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
