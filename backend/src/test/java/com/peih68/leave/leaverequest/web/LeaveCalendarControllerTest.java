package com.peih68.leave.leaverequest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.leaverequest.service.LeaveCalendarService;
import com.peih68.leave.user.domain.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LeaveCalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class LeaveCalendarControllerTest {

    @Autowired MockMvc mvc;
    @MockBean LeaveCalendarService calendarService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void authenticatedUserCanReadCalendar() throws Exception {
        given(calendarService.calendar(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of());
        mvc.perform(get("/calendar?from=2026-08-03&to=2026-08-07"))
                .andExpect(status().isOk());
    }
}
