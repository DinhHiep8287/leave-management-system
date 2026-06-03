package com.peih68.leave.leaverequest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.user.domain.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LeaveRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class LeaveRequestApprovalControllerTest {

    @Autowired MockMvc mvc;
    @MockBean LeaveRequestService leaveRequestService;
    @MockBean(name = "leaveRequestSecurity") LeaveRequestSecurity leaveRequestSecurity;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void manager_canApproveOwnTeamRequest() throws Exception {
        given(leaveRequestSecurity.isManagerOf(eq(5L), eq(3L))).willReturn(true);
        given(leaveRequestService.approve(eq(5L), any(), any())).willReturn(null);
        mvc.perform(post("/leave-requests/5/approve").with(csrf())
                        .contentType("application/json").content("{\"comment\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void manager_cannotApproveOtherTeamRequest() throws Exception {
        // isManagerOf defaults to false → 403
        mvc.perform(post("/leave-requests/5/approve").with(csrf())
                        .contentType("application/json").content("{\"comment\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_cannotApprove() throws Exception {
        mvc.perform(post("/leave-requests/5/approve").with(csrf())
                        .contentType("application/json").content("{\"comment\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hr_canReject() throws Exception {
        given(leaveRequestService.reject(eq(5L), any(), any())).willReturn(null);
        mvc.perform(post("/leave-requests/5/reject").with(csrf())
                        .contentType("application/json").content("{\"comment\":\"no\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void requester_canCancelOwnRequest() throws Exception {
        given(leaveRequestSecurity.isRequester(eq(5L), eq(7L))).willReturn(true);
        given(leaveRequestService.cancel(eq(5L), any(), any())).willReturn(null);
        mvc.perform(post("/leave-requests/5/cancel").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void nonParticipant_cannotCancel() throws Exception {
        // neither manager nor requester → 403
        mvc.perform(post("/leave-requests/5/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void participant_canViewHistory() throws Exception {
        given(leaveRequestSecurity.isParticipant(eq(5L), eq(7L))).willReturn(true);
        given(leaveRequestService.history(5L)).willReturn(List.of());
        mvc.perform(get("/leave-requests/5/history")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void nonParticipant_cannotViewHistory() throws Exception {
        mvc.perform(get("/leave-requests/5/history")).andExpect(status().isForbidden());
    }
}
