package com.peih68.leave.leaverequest.web.dto;

import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LeaveRequestResponse(
        Long id,
        Long userId,
        String userFullName,
        Long leaveTypeId,
        String leaveTypeCode,
        LocalDate startDate,
        LocalDate endDate,
        LeaveHalf startHalf,
        LeaveHalf endHalf,
        BigDecimal totalDays,
        String reason,
        LeaveStatus status,
        Long managerId,
        String managerName,
        OffsetDateTime createdAt) {}
