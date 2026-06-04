package com.peih68.leave.leaverequest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LeaveRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class LeaveRequestControllerTest {

    private static final String VALID_BODY = """
            {"leaveTypeId":1,"startDate":"2030-01-07","endDate":"2030-01-11",
             "startHalf":"FULL_DAY","endHalf":"FULL_DAY","reason":"vacation"}
            """;

    @Autowired MockMvc mvc;
    @MockBean LeaveRequestService leaveRequestService;
    // Name must match the @Component("leaveRequestSecurity") so the @PreAuthorize SpEL
    // bean reference @leaveRequestSecurity resolves in the slice.
    @MockBean(name = "leaveRequestSecurity") LeaveRequestSecurity leaveRequestSecurity;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_canSubmitOwnRequest() throws Exception {
        given(leaveRequestService.submit(any(), any())).willReturn(null);
        mvc.perform(post("/leave-requests").with(csrf())
                        .contentType("application/json").content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void requester_canEditOwnRequest() throws Exception {
        given(leaveRequestSecurity.isRequester(eq(5L), eq(7L))).willReturn(true);
        given(leaveRequestService.update(eq(5L), any(), any())).willReturn(null);
        mvc.perform(put("/leave-requests/5").with(csrf()).contentType("application/json").content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void nonRequester_cannotEditRequest() throws Exception {
        // leaveRequestSecurity.isRequester defaults to false → 403
        mvc.perform(put("/leave-requests/5").with(csrf()).contentType("application/json").content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void participant_canViewRequest() throws Exception {
        given(leaveRequestSecurity.isParticipant(eq(5L), eq(7L))).willReturn(true);
        given(leaveRequestService.findById(5L)).willReturn(null);
        mvc.perform(get("/leave-requests/5")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void nonParticipant_cannotViewRequest() throws Exception {
        // leaveRequestSecurity.isParticipant defaults to false → 403
        mvc.perform(get("/leave-requests/5")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hr_canViewAnyRequest() throws Exception {
        given(leaveRequestService.findById(5L)).willReturn(null);
        mvc.perform(get("/leave-requests/5")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_canListOwnRequests() throws Exception {
        mvc.perform(get("/users/7/leave-requests")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_cannotListOthersRequests() throws Exception {
        mvc.perform(get("/users/99/leave-requests")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void manager_canSeeInbox() throws Exception {
        given(leaveRequestService.listForApprover(any(), any(), any())).willReturn(Page.empty());
        mvc.perform(get("/leave-requests?status=PENDING")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employee_cannotSeeInbox() throws Exception {
        mvc.perform(get("/leave-requests?status=PENDING")).andExpect(status().isForbidden());
    }
}
