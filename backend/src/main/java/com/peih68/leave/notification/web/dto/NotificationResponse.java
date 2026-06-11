package com.peih68.leave.notification.web.dto;

import com.peih68.leave.leaverequest.domain.ApprovalAction;
import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        Long leaveRequestId,
        ApprovalAction eventType,
        String message,
        boolean isRead,
        OffsetDateTime createdAt) {}
