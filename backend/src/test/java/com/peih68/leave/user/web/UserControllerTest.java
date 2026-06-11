package com.peih68.leave.user.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.service.UserService;
import com.peih68.leave.user.web.dto.MeResponse;
import com.peih68.leave.user.web.dto.UserCreateRequest;
import com.peih68.leave.user.web.dto.UserResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean UserService userService;
    @MockBean JwtService jwtService;

    private static UserResponse sample(long id) {
        return new UserResponse(id, "E0010", "x@y.z", "X Y", Role.EMPLOYEE, 1L, null,
                LocalDate.of(2024, 1, 1), true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreate() throws Exception {
        given(userService.create(any(UserCreateRequest.class))).willReturn(sample(10L));
        mvc.perform(post("/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UserCreateRequest(
                                "E0010", "x@y.z", "X Y", "password1",
                                Role.EMPLOYEE, 1L, null, LocalDate.of(2024, 1, 1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotCreate() throws Exception {
        mvc.perform(post("/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UserCreateRequest(
                                "E0010", "x@y.z", "X Y", "password1",
                                Role.EMPLOYEE, 1L, null, LocalDate.of(2024, 1, 1)))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_validation_shortPassword() throws Exception {
        mvc.perform(post("/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UserCreateRequest(
                                "E0010", "x@y.z", "X Y", "short",
                                Role.EMPLOYEE, 1L, null, LocalDate.of(2024, 1, 1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotList() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void hr_canList() throws Exception {
        given(userService.list(any(), any(), any(), anyBoolean(), any())).willReturn(Page.empty());
        mvc.perform(get("/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canResetPassword() throws Exception {
        mvc.perform(post("/users/5/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPassword1\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotResetPassword() throws Exception {
        mvc.perform(post("/users/5/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPassword1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 5L, role = Role.EMPLOYEE)
    void me_returnsResolvedDepartmentAndManagerNames() throws Exception {
        given(userService.findMe(5L)).willReturn(new MeResponse(
                5L, "E0005", "e@x.com", "Eng Employee", Role.EMPLOYEE,
                1L, "Engineering", 3L, "Eng Manager",
                LocalDate.of(2025, 1, 1), true, OffsetDateTime.now(), OffsetDateTime.now()));

        mvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.data.managerName").value("Eng Manager"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canDeactivate() throws Exception {
        given(userService.setActive(eq(7L), eq(false))).willReturn(sample(7L));
        mvc.perform(post("/users/7/deactivate").with(csrf()))
                .andExpect(status().isOk());
    }
}
