package com.peih68.leave.leaverequest.domain;

/** Lifecycle status of a leave request. Matches the {@code status} CHECK constraint
 * on {@code leave_requests}. */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
