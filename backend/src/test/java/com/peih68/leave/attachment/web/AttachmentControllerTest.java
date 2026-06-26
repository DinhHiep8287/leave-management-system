package com.peih68.leave.attachment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.peih68.leave.attachment.service.AttachmentService;
import com.peih68.leave.auth.service.JwtService;
import com.peih68.leave.common.exception.GlobalExceptionHandler;
import com.peih68.leave.config.MethodSecurityTestConfig;
import com.peih68.leave.config.WithMockPrincipal;
import com.peih68.leave.user.domain.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class AttachmentControllerTest {

    @Autowired MockMvc mvc;
    @MockBean AttachmentService attachmentService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void canListAttachments() throws Exception {
        given(attachmentService.list(eq(5L), any())).willReturn(List.of());
        mvc.perform(get("/leave-requests/5/attachments")).andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void canUploadMultipartAttachments() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "doctor.pdf", "application/pdf", "%PDF".getBytes());
        given(attachmentService.upload(eq(5L), any(), any())).willReturn(List.of());
        mvc.perform(multipart("/leave-requests/5/attachments").file(file).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(id = 7L, role = Role.EMPLOYEE)
    void canDeleteAttachment() throws Exception {
        mvc.perform(delete("/leave-requests/5/attachments/9").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
