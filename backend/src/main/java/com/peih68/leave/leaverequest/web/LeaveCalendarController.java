package com.peih68.leave.leaverequest.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.leaverequest.service.LeaveCalendarService;
import com.peih68.leave.leaverequest.web.dto.CalendarEntryResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LeaveCalendarController {

    private final LeaveCalendarService calendarService;

    /**
     * Team leave calendar for [from, to]. Scope follows the caller's role; HR/ADMIN may
     * narrow to one department via {@code departmentId}.
     */
    @GetMapping("/calendar")
    public ApiResponse<List<CalendarEntryResponse>> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(name = "includePending", defaultValue = "false") boolean includePending,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(
                calendarService.calendar(principal, from, to, departmentId, leaveTypeId, userId, includePending));
    }
}
