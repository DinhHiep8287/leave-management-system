package com.peih68.leave.leavebalance.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.user.domain.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LeaveBalanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class LeaveBalanceControllerTest {

    @Autowired MockMvc mvc;
    @MockBean LeaveBalanceService leaveBalanceService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_canReadOwnBalances() throws Exception {
        given(leaveBalanceService.findByUser(eq(7L), anyInt())).willReturn(List.of());
        mvc.perform(get("/users/7/leave-balances?year=2026"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_cannotReadOthersBalances() throws Exception {
        mvc.perform(get("/users/99/leave-balances?year=2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hr_canReadOthersBalances() throws Exception {
        given(leaveBalanceService.findByUser(eq(99L), anyInt())).willReturn(List.of());
        mvc.perform(get("/users/99/leave-balances?year=2026"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_cannotInitialize() throws Exception {
        mvc.perform(post("/leave-balances/initialize?year=2026").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.ADMIN)
    void admin_canInitialize() throws Exception {
        given(leaveBalanceService.bulkInitializeYear(anyInt())).willReturn(15);
        mvc.perform(post("/leave-balances/initialize?year=2026").with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_cannotAdjust() throws Exception {
        mvc.perform(patch("/leave-balances/5/adjust").with(csrf())
                        .contentType("application/json")
                        .content("{\"adjustedDaysDelta\":1.0,\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hr_canAdjust() throws Exception {
        given(leaveBalanceService.adjust(eq(5L), any(), any())).willReturn(null);
        mvc.perform(patch("/leave-balances/5/adjust").with(csrf())
                        .contentType("application/json")
                        .content("{\"adjustedDaysDelta\":2.0,\"reason\":\"correction\"}"))
                .andExpect(status().isOk());
    }
}
