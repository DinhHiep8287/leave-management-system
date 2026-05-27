package com.peih68.leave.leavetype.repository;

import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveTypeEntity, Long> {

    Optional<LeaveTypeEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<LeaveTypeEntity> findByIsActiveTrueOrderByCodeAsc();

    List<LeaveTypeEntity> findByIsActiveTrueAndRequiresBalanceTrueOrderByCodeAsc();

    List<LeaveTypeEntity> findAllByOrderByCodeAsc();
}
