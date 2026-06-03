package com.peih68.leave.leaverequest.web.dto;

import com.peih68.leave.leaverequest.domain.LeaveHalf;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LeaveRequestCreateRequest(
        @NotNull Long leaveTypeId,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate endDate,
        @NotNull LeaveHalf startHalf,
        @NotNull LeaveHalf endHalf,
        @NotBlank @Size(max = 2000) String reason) {}
