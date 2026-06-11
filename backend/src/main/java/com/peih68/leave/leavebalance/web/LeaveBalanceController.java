package com.peih68.leave.leavebalance.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceAdjustRequest;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceResponse;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceUpsertRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    /** A user can read their own balances; HR/ADMIN can read anyone's. */
    @GetMapping("/users/{id}/leave-balances")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or #id == principal.id")
    public ApiResponse<List<LeaveBalanceResponse>> byUser(
            @PathVariable Long id,
            @RequestParam int year) {
        return ApiResponse.ok(leaveBalanceService.findByUser(id, year));
    }

    @PostMapping("/leave-balances")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeaveBalanceResponse> upsert(@Valid @RequestBody LeaveBalanceUpsertRequest req) {
        return ApiResponse.ok(leaveBalanceService.upsert(req));
    }

    @PostMapping("/leave-balances/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> initialize(@RequestParam int year) {
        int created = leaveBalanceService.bulkInitializeYear(year);
        return ApiResponse.ok(Map.of("year", year, "created", created));
    }

    /** Carries remaining leave of fromYear into fromYear+1, capped per (user, type). */
    @PostMapping("/leave-balances/carry-over")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> carryOver(
            @RequestParam int fromYear,
            @RequestParam(defaultValue = "5") java.math.BigDecimal capDays,
            @AuthenticationPrincipal UserPrincipal actor) {
        int carried = leaveBalanceService.carryOverYear(fromYear, capDays, actor);
        return ApiResponse.ok(Map.of("fromYear", fromYear, "capDays", capDays, "carried", carried));
    }

    @PatchMapping("/leave-balances/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<LeaveBalanceResponse> adjust(
            @PathVariable Long id,
            @Valid @RequestBody LeaveBalanceAdjustRequest req,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(leaveBalanceService.adjust(id, req, actor));
    }
}
