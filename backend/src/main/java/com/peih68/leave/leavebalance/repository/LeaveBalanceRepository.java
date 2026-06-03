package com.peih68.leave.leavebalance.repository;

import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, Long> {

    Optional<LeaveBalanceEntity> findByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, Integer year);

    boolean existsByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, Integer year);

    List<LeaveBalanceEntity> findByUserIdAndYearOrderByLeaveTypeId(Long userId, Integer year);

    /** All balances for a year, for the CSV report. */
    List<LeaveBalanceEntity> findByYearOrderByUserIdAscLeaveTypeIdAsc(Integer year);
}
