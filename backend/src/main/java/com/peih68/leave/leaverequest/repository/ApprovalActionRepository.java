package com.peih68.leave.leaverequest.repository;

import com.peih68.leave.leaverequest.domain.ApprovalActionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalActionRepository extends JpaRepository<ApprovalActionEntity, Long> {

    List<ApprovalActionEntity> findByLeaveRequestIdOrderByCreatedAtAsc(Long leaveRequestId);
}
