package com.peih68.leave.leaverequest.domain;

/** Workflow action recorded against a leave request. Matches the {@code action}
 * CHECK constraint on {@code approval_actions}. */
public enum ApprovalAction {
    CREATED,
    UPDATED,
    APPROVED,
    REJECTED,
    CANCELLED,
    OVERRIDE
}
