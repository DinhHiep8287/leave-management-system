package com.peih68.leave.department.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.department.service.DepartmentService;
import com.peih68.leave.department.web.dto.DepartmentRequest;
import com.peih68.leave.department.web.dto.DepartmentResponse;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;

@WebMvcTest(controllers = DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class DepartmentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean DepartmentService departmentService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreate() throws Exception {
        given(departmentService.create(any(DepartmentRequest.class)))
                .willReturn(new DepartmentResponse(1L, "QA", "QA Team", null, true,
                        OffsetDateTime.now(), OffsetDateTime.now()));

        mvc.perform(post("/departments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepartmentRequest("QA", "QA Team", null, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("QA"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotCreate() throws Exception {
        mvc.perform(post("/departments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepartmentRequest("QA", "QA Team", null, true))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manager_cannotDelete() throws Exception {
        mvc.perform(delete("/departments/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_validation_emptyName() throws Exception {
        mvc.perform(post("/departments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepartmentRequest("QA", "", null, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_validation_invalidCodeChars() throws Exception {
        mvc.perform(post("/departments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepartmentRequest("QA TEAM!", "X", null, true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canList() throws Exception {
        given(departmentService.list(any(), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .willReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/departments"))
                .andExpect(status().isOk());
        verify(departmentService).list(any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
    }
}
