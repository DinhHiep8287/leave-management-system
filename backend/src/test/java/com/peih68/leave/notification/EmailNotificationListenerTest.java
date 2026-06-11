package com.peih68.leave.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.event.LeaveRequestChangedEvent;
import com.peih68.leave.notification.service.EmailNotificationListener;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailNotificationListenerTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender s) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(s);
        return p;
    }

    private static LeaveRequestChangedEvent approvedEvent() {
        return new LeaveRequestChangedEvent(7L, 42L, ApprovalAction.APPROVED,
                "Đơn nghỉ 06/07–10/07 (ANNUAL) của bạn đã được duyệt");
    }

    @Test
    void sendsMailToRecipientWhenEnabled() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(UserEntity.builder()
                .email("emp@demo.local").fullName("Emp").role(Role.EMPLOYEE).build()));
        var listener = new EmailNotificationListener(provider(sender), userRepository, true, "noreply@x");

        listener.on(approvedEvent());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("emp@demo.local");
        assertThat(sent.getSubject()).contains("đã được duyệt");
        assertThat(sent.getText()).contains("06/07–10/07");
    }

    @Test
    void doesNothingWhenDisabled() {
        var listener = new EmailNotificationListener(provider(sender), userRepository, false, "noreply@x");
        listener.on(approvedEvent());
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void survivesMissingMailSender() {
        var listener = new EmailNotificationListener(provider(null), userRepository, true, "noreply@x");
        listener.on(approvedEvent()); // must not throw
    }
}
