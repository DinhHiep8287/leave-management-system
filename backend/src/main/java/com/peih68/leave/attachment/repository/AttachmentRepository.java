package com.peih68.leave.attachment.repository;

import com.peih68.leave.attachment.domain.AttachmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

    List<AttachmentEntity> findByLeaveRequestIdOrderByCreatedAtAsc(Long leaveRequestId);

    long countByLeaveRequestId(Long leaveRequestId);

    boolean existsByIdAndLeaveRequestId(Long id, Long leaveRequestId);
}
