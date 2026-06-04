package com.peih68.leave.holiday.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.holiday.web.dto.HolidayResponse;
import com.peih68.leave.user.domain.Role;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HolidayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class HolidayControllerTest {

    private static final String BODY =
            "{\"holidayDate\":\"2027-03-15\",\"name\":\"Ngày test\",\"description\":\"x\"}";

    @Autowired MockMvc mvc;
    @MockBean HolidayService holidayService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void anyoneCanListHolidays() throws Exception {
        given(holidayService.listByYear(2026)).willReturn(List.of());
        mvc.perform(get("/holidays?year=2026")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void employeeCannotCreate() throws Exception {
        mvc.perform(post("/holidays").with(csrf()).contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(id = 1L, role = Role.HR)
    void hrCanCreate() throws Exception {
        given(holidayService.create(any()))
                .willReturn(new HolidayResponse(1L, LocalDate.of(2027, 3, 15), "Ngày test", "x"));
        mvc.perform(post("/holidays").with(csrf()).contentType("application/json").content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockPrincipal(id = 2L, role = Role.ADMIN)
    void adminCanDelete() throws Exception {
        mvc.perform(delete("/holidays/5").with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void managerCannotDelete() throws Exception {
        mvc.perform(delete("/holidays/5").with(csrf())).andExpect(status().isForbidden());
    }
}
