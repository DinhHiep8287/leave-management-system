package com.peih68.leave.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestResponse;
import com.peih68.leave.notification.repository.NotificationRepository;
import com.peih68.leave.notification.service.NotificationService;
import com.peih68.leave.notification.web.dto.NotificationResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Lifecycle events must produce in-app notifications for the right recipient. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationFlowTest {

    private static final LocalDate MON = LocalDate.of(2026, 7, 6);
    private static final LocalDate FRI = LocalDate.of(2026, 7, 10);

    @Autowired LeaveRequestService requestService;
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired JdbcTemplate jdbc;

    private UserEntity manager;
    private UserEntity employee;
    private Long unpaidTypeId;

    @BeforeEach
    void setup() {
        Long engId = jdbc.queryForObject("SELECT id FROM departments WHERE code = 'ENG'", Long.class);
        manager = userRepository.save(UserEntity.builder()
                .employeeCode("NTF-MGR").email("ntfmgr@ex.com").passwordHash("x")
                .fullName("Ntf Mgr").role(Role.MANAGER).departmentId(engId)
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        employee = userRepository.save(UserEntity.builder()
                .employeeCode("NTF-EMP").email("ntfemp@ex.com").passwordHash("x")
                .fullName("Ntf Emp").role(Role.EMPLOYEE).departmentId(engId).managerId(manager.getId())
                .joinDate(LocalDate.of(2024, 1, 1)).isActive(true).build());
        unpaidTypeId = leaveTypeRepository.save(LeaveTypeEntity.builder()
                .code("NTF-UNPAID").name("Unpaid").defaultQuotaDays(new BigDecimal("0.0"))
                .requiresBalance(false).isActive(true).build()).getId();
    }

    private LeaveRequestResponse submit() {
        return requestService.submit(new LeaveRequestCreateRequest(
                        unpaidTypeId, MON, FRI, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, "ntf"),
                UserPrincipal.from(employee));
    }

    @Test
    void submitNotifiesManager_andApproveNotifiesRequester() {
        LeaveRequestResponse r = submit();

        var managerInbox = notificationService.list(manager.getId(), true, PageRequest.of(0, 10));
        assertThat(managerInbox.getContent())
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.eventType()).isEqualTo(ApprovalAction.CREATED);
                    assertThat(n.leaveRequestId()).isEqualTo(r.id());
                    assertThat(n.message()).contains("Ntf Emp").contains("chờ bạn duyệt");
                });

        requestService.approve(r.id(), null, UserPrincipal.from(manager));
        var employeeInbox = notificationService.list(employee.getId(), true, PageRequest.of(0, 10));
        assertThat(employeeInbox.getContent())
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.eventType()).isEqualTo(ApprovalAction.APPROVED);
                    assertThat(n.message()).contains("đã được duyệt");
                });
    }

    @Test
    void cancelByRequesterNotifiesManagerOnly() {
        LeaveRequestResponse r = submit();
        requestService.cancel(r.id(), null, UserPrincipal.from(employee));

        assertThat(notificationService.unreadCount(manager.getId())).isEqualTo(2); // CREATED + CANCELLED
        assertThat(notificationService.unreadCount(employee.getId())).isZero();
    }

    @Test
    void markReadIsOwnerOnly_andMarkAllReadClearsCount() {
        LeaveRequestResponse r = submit();
        NotificationResponse n = notificationService
                .list(manager.getId(), true, PageRequest.of(0, 10)).getContent().getFirst();

        assertThatThrownBy(() -> notificationService.markRead(n.id(), employee.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThat(notificationService.markRead(n.id(), manager.getId()).isRead()).isTrue();
        assertThat(notificationService.unreadCount(manager.getId())).isZero();

        // Another event, then read-all.
        requestService.approve(r.id(), null, UserPrincipal.from(manager));
        assertThat(notificationService.unreadCount(employee.getId())).isEqualTo(1);
        assertThat(notificationService.markAllRead(employee.getId())).isEqualTo(1);
        assertThat(notificationService.unreadCount(employee.getId())).isZero();
    }
}
