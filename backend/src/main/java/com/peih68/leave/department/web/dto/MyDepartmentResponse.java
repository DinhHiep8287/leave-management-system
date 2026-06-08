package com.peih68.leave.department.web.dto;

import java.util.List;

/** The current user's own department plus its active members (self-scoped, no id param). */
public record MyDepartmentResponse(
        Long id,
        String code,
        String name,
        Long headUserId,
        String headName,
        List<DepartmentMemberResponse> members) {}
