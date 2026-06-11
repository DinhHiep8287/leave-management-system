package com.peih68.leave.notification.repository;

import com.peih68.leave.notification.domain.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = TRUE, n.readAt = CURRENT_TIMESTAMP "
            + "WHERE n.userId = :userId AND n.isRead = FALSE")
    int markAllRead(@Param("userId") Long userId);
}
