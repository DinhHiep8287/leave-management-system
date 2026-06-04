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

    /** Like {@link #existsOverlap} but ignores one request (used when editing it). */
    @Query("""
            SELECT COUNT(r) > 0 FROM LeaveRequestEntity r
            WHERE r.userId = :userId
              AND r.status IN :statuses
              AND r.startDate <= :end
              AND r.endDate >= :start
              AND r.id <> :excludeId
            """)
    boolean existsOverlapExcluding(
            @Param("userId") Long userId,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("excludeId") Long excludeId);

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

    /** Calendar: requests for the given users whose date range overlaps [from, to]. */
    @Query("""
            SELECT r FROM LeaveRequestEntity r
            WHERE r.userId IN :userIds
              AND r.status IN :statuses
              AND r.startDate <= :to
              AND r.endDate >= :from
            ORDER BY r.startDate ASC
            """)
    List<LeaveRequestEntity> findOverlappingForUsers(
            @Param("userIds") Collection<Long> userIds,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Calendar (privileged, all users): requests whose date range overlaps [from, to]. */
    @Query("""
            SELECT r FROM LeaveRequestEntity r
            WHERE r.status IN :statuses
              AND r.startDate <= :to
              AND r.endDate >= :from
            ORDER BY r.startDate ASC
            """)
    List<LeaveRequestEntity> findOverlapping(
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Dashboard: number of requests per department, for requests overlapping [from, to]
     * with the given status. Returns rows of [departmentId, count], busiest first. */
    @Query("""
            SELECT u.departmentId, COUNT(r) FROM LeaveRequestEntity r, UserEntity u
            WHERE u.id = r.userId
              AND r.status = :status
              AND r.startDate <= :to
              AND r.endDate >= :from
            GROUP BY u.departmentId
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countByDepartmentForStatusInRange(
            @Param("status") LeaveStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Report: requests of a status whose start date falls within [from, to]. */
    List<LeaveRequestEntity> findByStatusAndStartDateBetween(
            LeaveStatus status, LocalDate from, LocalDate to);

    // Dashboard counters.
    long countByStatus(LeaveStatus status);

    long countByManagerIdAndStatus(Long managerId, LeaveStatus status);

    long countByUserIdAndStatus(Long userId, LeaveStatus status);
}
