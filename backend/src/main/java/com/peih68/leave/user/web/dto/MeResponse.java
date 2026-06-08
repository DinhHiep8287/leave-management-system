package com.peih68.leave.user.web.dto;

import com.peih68.leave.user.domain.Role;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Profile view for the current user (/users/me). Unlike {@link UserResponse}, it resolves
 * the department and manager names so the profile screen can show them without extra lookups.
 */
public record MeResponse(
        Long id,
        String employeeCode,
        String email,
        String fullName,
        Role role,
        Long departmentId,
        String departmentName,
        Long managerId,
        String managerName,
        LocalDate joinDate,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
