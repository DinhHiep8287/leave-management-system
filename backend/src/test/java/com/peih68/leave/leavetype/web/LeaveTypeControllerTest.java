package com.peih68.leave.leavetype.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.leavetype.service.LeaveTypeService;
import com.peih68.leave.leavetype.web.dto.LeaveTypeRequest;
import com.peih68.leave.leavetype.web.dto.LeaveTypeResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LeaveTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class LeaveTypeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean LeaveTypeService leaveTypeService;
    @MockBean JwtService jwtService;

    private static LeaveTypeResponse sample() {
        return new LeaveTypeResponse(1L, "WFH", "Work From Home", null,
                new BigDecimal("5.0"), true, true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreate() throws Exception {
        given(leaveTypeService.create(any(LeaveTypeRequest.class))).willReturn(sample());
        mvc.perform(post("/leave-types").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LeaveTypeRequest(
                                "WFH", "Work From Home", null, new BigDecimal("5.0"), true, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("WFH"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotCreate() throws Exception {
        mvc.perform(post("/leave-types").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LeaveTypeRequest(
                                "WFH", "Work From Home", null, new BigDecimal("5.0"), true, true))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canList() throws Exception {
        given(leaveTypeService.list(org.mockito.ArgumentMatchers.anyBoolean()))
                .willReturn(List.of(sample()));
        mvc.perform(get("/leave-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("WFH"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validation_negativeQuota() throws Exception {
        mvc.perform(post("/leave-types").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LeaveTypeRequest(
                                "WFH", "Work From Home", null, new BigDecimal("-1.0"), true, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validation_tooManyFractionDigits() throws Exception {
        mvc.perform(post("/leave-types").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LeaveTypeRequest(
                                "WFH", "Work From Home", null, new BigDecimal("5.25"), true, true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validation_invalidCode() throws Exception {
        mvc.perform(post("/leave-types").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LeaveTypeRequest(
                                "WFH TYPE!", "X", null, new BigDecimal("5.0"), true, true))))
                .andExpect(status().isBadRequest());
    }
}
