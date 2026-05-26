package com.peih68.leave.department.web.dto;

import java.time.OffsetDateTime;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        Long headUserId,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
