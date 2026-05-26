package com.peih68.leave.auth.web.dto;

public record MeResponse(
        Long id,
        String email,
        String fullName,
        String role,
        Long departmentId,
        Long managerId,
        boolean active) {}
