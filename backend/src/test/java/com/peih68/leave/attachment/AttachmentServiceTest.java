package com.peih68.leave.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.attachment.service.AttachmentDownload;
import com.peih68.leave.attachment.service.AttachmentService;
import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.attachments.enabled=true",
        "app.attachments.storage-dir=${java.io.tmpdir}/leave-attachment-test"
})
@ActiveProfiles("test")
@Transactional
class AttachmentServiceTest {

    private static final LocalDate MON = LocalDate.of(2026, 7, 6);

    @Autowired AttachmentService attachmentService;
    @Autowired LeaveRequestService leaveRequestService;
    @Autowired LeaveRequestRepository leaveRequestRepository;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired JdbcTemplate jdbc;

    private UserEntity manager;
    private UserEntity employee;
    private Long typeId;
    private Long requestId;

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        manager = userRepository.save(UserEntity.builder()
                .employeeCode("ATT-MGR").email("attmgr@example.com").passwordHash("x")
                .fullName("Attachment Manager").role(Role.MANAGER).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        employee = userRepository.save(UserEntity.builder()
                .employeeCode("ATT-EMP").email("attemp@example.com").passwordHash("x")
                .fullName("Attachment Employee").role(Role.EMPLOYEE).departmentId(engId)
                .managerId(manager.getId()).joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        typeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("ATT-UNPAID").name("Attachment unpaid").defaultQuotaDays(BigDecimal.ZERO)
                .requiresBalance(false).isActive(true).build()).getId();
        requestId = leaveRequestService.submit(new LeaveRequestCreateRequest(
                typeId, MON, MON, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, "reason"),
                UserPrincipal.from(employee)).id();
    }

    @Test
    void requesterCanUploadListAndDownloadPendingAttachment() {
        var uploaded = attachmentService.upload(requestId, java.util.List.of(pdf("doctor.pdf")), UserPrincipal.from(employee));

        assertThat(uploaded).singleElement().satisfies(a -> {
            assertThat(a.originalFilename()).isEqualTo("doctor.pdf");
            assertThat(a.contentType()).isEqualTo("application/pdf");
            assertThat(a.uploadedById()).isEqualTo(employee.getId());
        });

        assertThat(attachmentService.list(requestId, UserPrincipal.from(manager))).hasSize(1);
        AttachmentDownload download = attachmentService.download(requestId, uploaded.get(0).id(), UserPrincipal.from(manager));
        assertThat(download.resource().exists()).isTrue();
    }

    @Test
    void rejectsUnsupportedTypeAndTooManyFiles() {
        assertThatThrownBy(() -> attachmentService.upload(requestId,
                java.util.List.of(new MockMultipartFile("files", "note.txt", "text/plain", "x".getBytes())),
                UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        assertThatThrownBy(() -> attachmentService.upload(requestId,
                java.util.List.of(pdf("1.pdf"), pdf("2.pdf"), pdf("3.pdf"), pdf("4.pdf"), pdf("5.pdf"), pdf("6.pdf")),
                UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void nonRequesterCannotUploadAndApprovedRequestIsLocked() {
        assertThatThrownBy(() -> attachmentService.upload(requestId, java.util.List.of(pdf("a.pdf")), UserPrincipal.from(manager)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.FORBIDDEN));

        leaveRequestRepository.findById(requestId).orElseThrow().setStatus(LeaveStatus.APPROVED);
        assertThatThrownBy(() -> attachmentService.upload(requestId, java.util.List.of(pdf("a.pdf")), UserPrincipal.from(employee)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void requesterCanDeletePendingAttachment() {
        Long attachmentId = attachmentService.upload(requestId, java.util.List.of(pdf("doctor.pdf")), UserPrincipal.from(employee))
                .get(0).id();

        attachmentService.delete(requestId, attachmentId, UserPrincipal.from(employee));

        assertThat(attachmentService.list(requestId, UserPrincipal.from(employee))).isEmpty();
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", "%PDF-1.4".getBytes());
    }
}
