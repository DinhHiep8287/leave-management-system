package com.peih68.leave.auth.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.auth.service.AuthService;
import com.peih68.leave.auth.web.dto.LoginRequest;
import com.peih68.leave.auth.web.dto.MeResponse;
import com.peih68.leave.auth.web.dto.RefreshRequest;
import com.peih68.leave.auth.web.dto.TokenPairResponse;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required");
        }
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "User not found"));
        return ApiResponse.ok(new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getDepartmentId(),
                user.getManagerId(),
                Boolean.TRUE.equals(user.getIsActive())));
    }
}
