package com.peih68.leave.leaverequest.repository;

import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

    /** True if the user already has a non-terminal request whose date range overlaps [start, end]. */
    @Query("""
            SELECT COUNT(r) > 0 FROM LeaveRequestEntity r
            WHERE r.userId = :userId
              AND r.status IN :statuses
              AND r.startDate <= :end
              AND r.endDate >= :start
            """)
    boolean existsOverlap(
            @Param("userId") Long userId,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** A single user's requests whose start date falls within [from, to], newest first.
     * Status filtering is applied in the service (the list per user is small). */
    List<LeaveRequestEntity> findByUserIdAndStartDateBetweenOrderByStartDateDesc(
            Long userId, LocalDate from, LocalDate to);

    // Approver inbox queries.
    Page<LeaveRequestEntity> findByManagerIdAndStatusOrderByStartDateAsc(
            Long managerId, LeaveStatus status, Pageable pageable);

    Page<LeaveRequestEntity> findByManagerIdOrderByStartDateAsc(Long managerId, Pageable pageable);

    Page<LeaveRequestEntity> findByStatusOrderByStartDateAsc(LeaveStatus status, Pageable pageable);

    Page<LeaveRequestEntity> findAllByOrderByStartDateAsc(Pageable pageable);
}
