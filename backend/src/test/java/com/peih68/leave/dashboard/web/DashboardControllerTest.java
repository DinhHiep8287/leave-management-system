package com.peih68.leave.dashboard.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.dashboard.service.DashboardService;
import com.peih68.leave.dashboard.web.dto.DashboardSummaryResponse;
import com.peih68.leave.user.domain.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class DashboardControllerTest {

    @Autowired MockMvc mvc;
    @MockBean DashboardService dashboardService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void authenticatedUserGetsSummary() throws Exception {
        given(dashboardService.summary(any()))
                .willReturn(new DashboardSummaryResponse(2L, 1, 1, 0L, List.of(), List.of()));
        mvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingApprovalCount").value(2))
                .andExpect(jsonPath("$.data.onLeaveTodayCount").value(1));
    }
}
