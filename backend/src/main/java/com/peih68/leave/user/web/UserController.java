package com.peih68.leave.user.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.service.UserService;
import com.peih68.leave.user.web.dto.ChangePasswordRequest;
import com.peih68.leave.user.web.dto.MeResponse;
import com.peih68.leave.user.web.dto.ResetPasswordRequest;
import com.peih68.leave.user.web.dto.UpdateMeRequest;
import com.peih68.leave.user.web.dto.UserCreateRequest;
import com.peih68.leave.user.web.dto.UserResponse;
import com.peih68.leave.user.web.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return ApiResponse.ok(userService.create(req));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.findMe(principal.getId()));
    }

    @PatchMapping("/me")
    public ApiResponse<MeResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMeRequest req) {
        userService.updateSelf(principal.getId(), req);
        return ApiResponse.ok(userService.findMe(principal.getId()));
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(principal.getId(), req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or #id == principal.id")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest req) {
        return ApiResponse.ok(userService.update(id, req));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok(userService.setActive(id, true));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(userService.setActive(id, false));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Page<UserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Role role,
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20, sort = "employeeCode") Pageable pageable) {
        Page<UserResponse> page = userService.list(q, departmentId, role, activeOnly, pageable);
        return ApiResponse.ok(page, Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()));
    }
}
