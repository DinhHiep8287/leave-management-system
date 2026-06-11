package com.peih68.leave.notification.service;

import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.notification.domain.NotificationEntity;
import com.peih68.leave.notification.repository.NotificationRepository;
import com.peih68.leave.notification.web.dto.NotificationResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /** Creates a notification; called inside the leave-request transaction so it is atomic. */
    @Transactional
    public void notify(Long userId, Long leaveRequestId, ApprovalAction eventType, String message) {
        if (userId == null) {
            return; // e.g. a request whose manager is gone — nothing to notify
        }
        repository.save(NotificationEntity.builder()
                .userId(userId)
                .leaveRequestId(leaveRequestId)
                .eventType(eventType)
                .message(message)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<NotificationEntity> page = unreadOnly
                ? repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    /** Marks one notification read; only the owner may do so. */
    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        NotificationEntity n = repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Notification not found: " + id));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "not your notification");
        }
        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setReadAt(OffsetDateTime.now());
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId);
    }

    private static NotificationResponse toResponse(NotificationEntity n) {
        return new NotificationResponse(
                n.getId(), n.getLeaveRequestId(), n.getEventType(), n.getMessage(),
                Boolean.TRUE.equals(n.getIsRead()), n.getCreatedAt());
    }
}
