package com.peih68.leave.department.web.dto;

import com.peih68.leave.user.domain.Role;

/** One active member of a department, for the "my department" roster. */
public record DepartmentMemberResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean isHead) {}
