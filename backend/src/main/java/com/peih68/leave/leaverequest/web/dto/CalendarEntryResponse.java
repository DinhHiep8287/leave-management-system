package com.peih68.leave.leaverequest.web.dto;

import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import java.time.LocalDate;

/** A single leave occurrence shown on the team calendar. */
public record CalendarEntryResponse(
        Long leaveRequestId,
        Long userId,
        String userFullName,
        Long departmentId,
        String departmentName,
        String leaveTypeCode,
        LocalDate startDate,
        LocalDate endDate,
        LeaveHalf startHalf,
        LeaveHalf endHalf,
        LeaveStatus status) {}
