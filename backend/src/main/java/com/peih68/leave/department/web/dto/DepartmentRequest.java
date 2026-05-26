package com.peih68.leave.department.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "code may only contain letters, digits, _ and -")
        String code,
        @NotBlank @Size(min = 1, max = 200) String name,
        Long headUserId,
        Boolean isActive) {}
