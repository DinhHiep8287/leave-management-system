package com.peih68.leave.user.web.dto;

import com.peih68.leave.user.domain.Role;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String employeeCode,
        String email,
        String fullName,
        Role role,
        Long departmentId,
        Long managerId,
        LocalDate joinDate,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
