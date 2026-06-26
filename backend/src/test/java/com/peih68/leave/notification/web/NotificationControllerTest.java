package com.peih68.leave.notification.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.notification.service.NotificationService;
import com.peih68.leave.notification.web.dto.NotificationResponse;
import com.peih68.leave.user.domain.Role;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class NotificationControllerTest {

    @Autowired MockMvc mvc;
    @MockBean NotificationService notificationService;
    @MockBean JwtService jwtService;

    private static NotificationResponse sample() {
        return new NotificationResponse(9L, 4L, ApprovalAction.CREATED,
                "Nhân viên nộp đơn nghỉ 06/07–10/07 (ANNUAL), đang chờ bạn duyệt",
                false, OffsetDateTime.now(), null);
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void listReturnsOwnNotifications() throws Exception {
        given(notificationService.list(eq(3L), anyBoolean(), any()))
                .willReturn(new PageImpl<>(List.of(sample())));
        mvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.MANAGER)
    void unreadCountUsesPrincipalId() throws Exception {
        given(notificationService.unreadCount(3L)).willReturn(2L);
        mvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @WithMockPrincipal(id = 3L, role = Role.EMPLOYEE)
    void markReadDelegatesWithPrincipalId() throws Exception {
        given(notificationService.markRead(9L, 3L)).willReturn(new NotificationResponse(
                9L, 4L, ApprovalAction.APPROVED, "x", true, OffsetDateTime.now(), OffsetDateTime.now()));
        mvc.perform(patch("/notifications/9/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").exists());
    }
}
