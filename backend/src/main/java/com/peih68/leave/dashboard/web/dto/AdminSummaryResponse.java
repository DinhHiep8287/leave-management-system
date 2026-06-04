package com.peih68.leave.dashboard.web.dto;

import java.util.List;

/** HR/ADMIN dashboard figures (REQUIREMENTS §10.3). */
public record AdminSummaryResponse(
        long totalActiveEmployees,
        long pendingCount,
        long approvedCount,
        long rejectedCount,
        long cancelledCount,
        List<DepartmentLeaveCount> topDepartmentsThisMonth) {

    public record DepartmentLeaveCount(Long departmentId, String departmentName, long requestCount) {}
}
