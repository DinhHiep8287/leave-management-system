package com.peih68.leave.report.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.report.service.ReportService;
import com.peih68.leave.report.web.dto.LeaveSummaryRow;
import com.peih68.leave.user.domain.Role;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class ReportControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ReportService reportService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employeeCannotDownloadRequests() throws Exception {
        mvc.perform(get("/reports/leave-requests.csv?from=2026-01-01&to=2026-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void managerCannotDownloadBalances() throws Exception {
        mvc.perform(get("/reports/leave-balances.csv?year=2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hrCanDownloadRequestsAsCsvAttachment() throws Exception {
        given(reportService.leaveRequestsCsv(any(), any(), any(), any())).willReturn("\uFEFFid\r\n1\r\n");
        mvc.perform(get("/reports/leave-requests.csv?from=2026-01-01&to=2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    @WithMockPrincipal(id = 2L, role = Role.ADMIN)
    void adminCanDownloadBalances() throws Exception {
        given(reportService.leaveBalancesCsv(anyInt(), isNull())).willReturn("\uFEFFuserFullName\r\n");
        mvc.perform(get("/reports/leave-balances.csv?year=2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hrCanDownloadSummary() throws Exception {
        given(reportService.leaveSummaryCsv(anyInt(), any(), isNull())).willReturn("\uFEFFmonth\r\n");
        mvc.perform(get("/reports/leave-summary.csv?year=2026&groupBy=month"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hrCanPreviewSummaryAsJson() throws Exception {
        given(reportService.leaveSummary(2026, "month", 4L))
                .willReturn(List.of(new LeaveSummaryRow("09", "ANNUAL", new BigDecimal("3.0"), 2)));
        mvc.perform(get("/reports/leave-summary?year=2026&groupBy=month&departmentId=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].period").value("09"))
                .andExpect(jsonPath("$.data[0].requestCount").value(2));
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employeeCannotDownloadSummary() throws Exception {
        mvc.perform(get("/reports/leave-summary.csv?year=2026")).andExpect(status().isForbidden());
    }
}
