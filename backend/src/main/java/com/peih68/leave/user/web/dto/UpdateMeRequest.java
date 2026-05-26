package com.peih68.leave.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-update — only fields a user is allowed to change about themselves. */
public record UpdateMeRequest(@NotBlank @Size(min = 1, max = 200) String fullName) {}
