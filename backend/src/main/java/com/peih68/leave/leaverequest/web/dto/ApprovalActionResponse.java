package com.peih68.leave.leaverequest.web.dto;

import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import java.time.OffsetDateTime;

public record ApprovalActionResponse(
        Long id,
        ApprovalAction action,
        Long actorId,
        String actorName,
        LeaveStatus previousStatus,
        LeaveStatus newStatus,
        String comment,
        OffsetDateTime createdAt) {}
