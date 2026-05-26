package com.peih68.leave.user.web.dto;

import com.peih68.leave.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserCreateRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$",
                message = "employeeCode may only contain letters, digits, _ and -")
        String employeeCode,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 1, max = 200) String fullName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Role role,
        @NotNull Long departmentId,
        Long managerId,
        @NotNull LocalDate joinDate) {}
