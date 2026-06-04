package com.peih68.leave.dashboard.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.dashboard.service.DashboardService;
import com.peih68.leave.dashboard.web.dto.AdminSummaryResponse;
import com.peih68.leave.dashboard.web.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** Role-aware summary figures for the authenticated user. */
    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(dashboardService.summary(principal));
    }

    /** Organisation-wide figures for HR/ADMIN (REQUIREMENTS §10.3). */
    @GetMapping("/dashboard/admin-summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<AdminSummaryResponse> adminSummary() {
        return ApiResponse.ok(dashboardService.adminSummary());
    }
}
