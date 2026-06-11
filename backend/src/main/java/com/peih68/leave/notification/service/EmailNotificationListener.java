package com.peih68.leave.notification.service;

import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.event.LeaveRequestChangedEvent;
import com.peih68.leave.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends a plain-text email for each leave-request lifecycle event, asynchronously and
 * only after the business transaction committed. Disabled by default ({@code
 * app.mail.enabled=false}) so production runs without an SMTP account; the dev profile
 * enables it against the Mailpit container.
 */
@Slf4j
@Component
public class EmailNotificationListener {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final UserRepository userRepository;
    private final boolean enabled;
    private final String from;

    public EmailNotificationListener(
            ObjectProvider<JavaMailSender> mailSender,
            UserRepository userRepository,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@leave.local}") String from) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.enabled = enabled;
        this.from = from;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(LeaveRequestChangedEvent event) {
        if (!enabled || event.recipientUserId() == null) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("app.mail.enabled=true but no JavaMailSender configured (set spring.mail.host)");
            return;
        }
        userRepository.findById(event.recipientUserId()).ifPresent(user -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(user.getEmail());
                msg.setSubject("[Nghỉ phép] " + subjectFor(event.action()));
                msg.setText(event.message() + "\n\nĐăng nhập hệ thống để xem chi tiết.");
                sender.send(msg);
            } catch (Exception e) {
                // Email is best-effort; the in-app notification is already persisted.
                log.warn("failed to send notification email to {}: {}", user.getEmail(), e.getMessage());
            }
        });
    }

    private static String subjectFor(ApprovalAction action) {
        return switch (action) {
            case CREATED -> "Có đơn mới chờ bạn duyệt";
            case UPDATED -> "Đơn chờ duyệt vừa được sửa";
            case APPROVED -> "Đơn của bạn đã được duyệt";
            case REJECTED -> "Đơn của bạn bị từ chối";
            case CANCELLED -> "Một đơn nghỉ đã bị hủy";
            default -> "Cập nhật đơn nghỉ phép";
        };
    }
}
