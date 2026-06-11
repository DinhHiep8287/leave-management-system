package com.peih68.leave.leaverequest.event;

import com.peih68.leave.leaverequest.domain.ApprovalAction;

/**
 * Published on every leave-request lifecycle transition, after the in-app notification
 * is written. Consumed asynchronously AFTER COMMIT (e.g. by the email listener), so
 * side effects never block or roll back the business transaction.
 */
public record LeaveRequestChangedEvent(
        Long recipientUserId,
        Long leaveRequestId,
        ApprovalAction action,
        String message) {}
