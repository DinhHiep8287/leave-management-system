package com.peih68.leave.leaverequest.domain;

/** Which portion of a boundary day a leave covers. Matches the {@code start_half}/{@code end_half}
 * CHECK constraint on {@code leave_requests}. */
public enum LeaveHalf {
    FULL_DAY,
    MORNING,
    AFTERNOON
}
