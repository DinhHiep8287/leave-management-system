package com.peih68.leave.user.service;

import com.peih68.leave.auth.repository.RefreshTokenRepository;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.department.domain.DepartmentEntity;
import com.peih68.leave.department.repository.DepartmentRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import com.peih68.leave.user.web.dto.ChangePasswordRequest;
import com.peih68.leave.user.web.dto.MeResponse;
import com.peih68.leave.user.web.dto.ResetPasswordRequest;
import com.peih68.leave.user.web.dto.UpdateMeRequest;
import com.peih68.leave.user.web.dto.UserCreateRequest;
import com.peih68.leave.user.web.dto.UserResponse;
import com.peih68.leave.user.web.dto.UserUpdateRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(ErrorCode.CONFLICT, "Email already in use: " + req.email());
        }
        if (userRepository.existsByEmployeeCodeIgnoreCase(req.employeeCode())) {
            throw new ApiException(ErrorCode.CONFLICT, "Employee code already in use: " + req.employeeCode());
        }
        validateDepartment(req.departmentId());
        validateManager(req.managerId());

        UserEntity entity = UserEntity.builder()
                .employeeCode(req.employeeCode())
                .email(req.email().toLowerCase())
                .fullName(req.fullName().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .departmentId(req.departmentId())
                .managerId(req.managerId())
                .joinDate(req.joinDate())
                .isActive(true)
                .build();
        return toResponse(userRepository.save(entity));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        UserEntity entity = findOrThrow(id);
        if (!entity.getEmail().equalsIgnoreCase(req.email())
                && userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(ErrorCode.CONFLICT, "Email already in use: " + req.email());
        }
        if (!entity.getEmployeeCode().equalsIgnoreCase(req.employeeCode())
                && userRepository.existsByEmployeeCodeIgnoreCase(req.employeeCode())) {
            throw new ApiException(ErrorCode.CONFLICT, "Employee code already in use: " + req.employeeCode());
        }
        validateDepartment(req.departmentId());
        validateManager(req.managerId());
        if (req.managerId() != null && req.managerId().equals(id)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "User cannot be their own manager");
        }

        entity.setEmployeeCode(req.employeeCode());
        entity.setEmail(req.email().toLowerCase());
        entity.setFullName(req.fullName().trim());
        entity.setRole(req.role());
        entity.setDepartmentId(req.departmentId());
        entity.setManagerId(req.managerId());
        entity.setJoinDate(req.joinDate());
        return toResponse(entity);
    }

    @Transactional
    public UserResponse updateSelf(Long id, UpdateMeRequest req) {
        UserEntity entity = findOrThrow(id);
        entity.setFullName(req.fullName().trim());
        return toResponse(entity);
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest req) {
        UserEntity entity = findOrThrow(id);
        if (!passwordEncoder.matches(req.oldPassword(), entity.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Old password is incorrect");
        }
        entity.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        // Force re-login on other devices
        refreshTokenRepository.revokeAllByUserId(entity.getId(), now());
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        UserEntity entity = findOrThrow(id);
        entity.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllByUserId(entity.getId(), now());
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        UserEntity entity = findOrThrow(id);
        if (Boolean.valueOf(active).equals(entity.getIsActive())) {
            return toResponse(entity);
        }
        entity.setIsActive(active);
        if (!active) {
            refreshTokenRepository.revokeAllByUserId(entity.getId(), now());
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /** Profile view: resolves department + manager names for /users/me. */
    @Transactional(readOnly = true)
    public MeResponse findMe(Long id) {
        UserEntity e = findOrThrow(id);
        String departmentName = departmentRepository.findById(e.getDepartmentId())
                .map(DepartmentEntity::getName).orElse(null);
        String managerName = e.getManagerId() == null ? null
                : userRepository.findById(e.getManagerId()).map(UserEntity::getFullName).orElse(null);
        return new MeResponse(
                e.getId(), e.getEmployeeCode(), e.getEmail(), e.getFullName(),
                e.getRole(), e.getDepartmentId(), departmentName,
                e.getManagerId(), managerName, e.getJoinDate(),
                Boolean.TRUE.equals(e.getIsActive()), e.getCreatedAt(), e.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String q, Long departmentId, Role role, boolean activeOnly, Pageable pageable) {
        return userRepository.search(q, departmentId, role, activeOnly, pageable).map(UserService::toResponse);
    }

    private UserEntity findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found: " + id));
    }

    private void validateDepartment(Long id) {
        if (id == null) return;
        if (!departmentRepository.existsById(id)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "departmentId does not exist: " + id);
        }
    }

    private void validateManager(Long managerId) {
        if (managerId == null) return;
        if (!userRepository.existsById(managerId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "managerId does not exist: " + managerId);
        }
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static UserResponse toResponse(UserEntity e) {
        return new UserResponse(
                e.getId(), e.getEmployeeCode(), e.getEmail(), e.getFullName(),
                e.getRole(), e.getDepartmentId(), e.getManagerId(), e.getJoinDate(),
                Boolean.TRUE.equals(e.getIsActive()), e.getCreatedAt(), e.getUpdatedAt());
    }
}
