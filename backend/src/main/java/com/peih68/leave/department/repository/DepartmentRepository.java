package com.peih68.leave.department.repository;

import com.peih68.leave.department.domain.DepartmentEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    Optional<DepartmentEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            SELECT d FROM DepartmentEntity d
            WHERE (:activeOnly = FALSE OR d.isActive = TRUE)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(d.code) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<DepartmentEntity> search(
            @Param("q") String q,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable);
}
