package com.peih68.leave.report.web;

import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.report.service.ReportService;
import com.peih68.leave.report.web.dto.LeaveSummaryRow;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CSV report downloads. Restricted to HR/ADMIN. */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService reportService;

    @GetMapping(value = "/leave-requests.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<String> leaveRequests(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long departmentId) {
        String csv = reportService.leaveRequestsCsv(from, to, status, departmentId);
        String filename = "leave-requests_%s_%s.csv".formatted(from, to);
        return csvResponse(csv, filename);
    }

    /** Total approved days per month/quarter and leave type for a year (REQUIREMENTS §11). */
    @GetMapping(value = "/leave-summary.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<String> leaveSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(name = "groupBy", defaultValue = "month") String groupBy,
            @RequestParam(required = false) Long departmentId) {
        int target = year != null ? year : Year.now().getValue();
        String csv = reportService.leaveSummaryCsv(target, groupBy, departmentId);
        return csvResponse(csv, "leave-summary_%d_%s.csv".formatted(target, groupBy));
    }

    /** JSON preview for the advanced report page. */
    @GetMapping("/leave-summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<List<LeaveSummaryRow>> leaveSummaryPreview(
            @RequestParam(required = false) Integer year,
            @RequestParam(name = "groupBy", defaultValue = "month") String groupBy,
            @RequestParam(required = false) Long departmentId) {
        int target = year != null ? year : Year.now().getValue();
        return ApiResponse.ok(reportService.leaveSummary(target, groupBy, departmentId));
    }

    @GetMapping(value = "/leave-balances.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<String> leaveBalances(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long departmentId) {
        int target = year != null ? year : Year.now().getValue();
        String csv = reportService.leaveBalancesCsv(target, departmentId);
        return csvResponse(csv, "leave-balances_%d.csv".formatted(target));
    }

    private ResponseEntity<String> csvResponse(String body, String filename) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
