package com.peih68.leave.user.repository;

import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE (:activeOnly = FALSE OR u.isActive = TRUE)
              AND (:departmentId IS NULL OR u.departmentId = :departmentId)
              AND (:role IS NULL OR u.role = :role)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.employeeCode) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<UserEntity> search(
            @Param("q") String q,
            @Param("departmentId") Long departmentId,
            @Param("role") Role role,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable);

    /** Active members of a department (calendar/report scoping). */
    List<UserEntity> findByDepartmentIdAndIsActiveTrue(Long departmentId);

    /** A manager's active direct reports (approver-scope calendar). */
    List<UserEntity> findByManagerIdAndIsActiveTrue(Long managerId);
}
