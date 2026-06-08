package com.peih68.leave.department.service;

import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.department.domain.DepartmentEntity;
import com.peih68.leave.department.repository.DepartmentRepository;
import com.peih68.leave.department.web.dto.DepartmentMemberResponse;
import com.peih68.leave.department.web.dto.DepartmentRequest;
import com.peih68.leave.department.web.dto.DepartmentResponse;
import com.peih68.leave.department.web.dto.MyDepartmentResponse;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String normalizedCode = request.code().toUpperCase();
        if (departmentRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ApiException(ErrorCode.CONFLICT, "Department code already exists: " + normalizedCode);
        }
        validateHeadUser(request.headUserId());
        DepartmentEntity entity = DepartmentEntity.builder()
                .code(normalizedCode)
                .name(request.name().trim())
                .headUserId(request.headUserId())
                .isActive(request.isActive() == null ? Boolean.TRUE : request.isActive())
                .build();
        return toResponse(departmentRepository.save(entity));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        DepartmentEntity entity = findOrThrow(id);
        String normalizedCode = request.code().toUpperCase();
        if (!entity.getCode().equalsIgnoreCase(normalizedCode)
                && departmentRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ApiException(ErrorCode.CONFLICT, "Department code already exists: " + normalizedCode);
        }
        validateHeadUser(request.headUserId());
        entity.setCode(normalizedCode);
        entity.setName(request.name().trim());
        entity.setHeadUserId(request.headUserId());
        if (request.isActive() != null) entity.setIsActive(request.isActive());
        return toResponse(entity);
    }

    @Transactional
    public void softDelete(Long id) {
        DepartmentEntity entity = findOrThrow(id);
        entity.setIsActive(false);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> list(String q, boolean activeOnly, Pageable pageable) {
        return departmentRepository.search(q, activeOnly, pageable).map(DepartmentService::toResponse);
    }

    /** The caller's own department + active members (head first, then by name). */
    @Transactional(readOnly = true)
    public MyDepartmentResponse myDepartment(Long userId) {
        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found: " + userId));
        DepartmentEntity dept = findOrThrow(me.getDepartmentId());
        Long headId = dept.getHeadUserId();
        String headName = headId == null ? null
                : userRepository.findById(headId).map(UserEntity::getFullName).orElse(null);
        List<DepartmentMemberResponse> members = userRepository
                .findByDepartmentIdAndIsActiveTrue(dept.getId()).stream()
                .sorted(Comparator
                        .comparing((UserEntity u) -> !u.getId().equals(headId)) // head first
                        .thenComparing(UserEntity::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> new DepartmentMemberResponse(
                        u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                        u.getId().equals(headId)))
                .toList();
        return new MyDepartmentResponse(
                dept.getId(), dept.getCode(), dept.getName(), headId, headName, members);
    }

    private DepartmentEntity findOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Department not found: " + id));
    }

    private void validateHeadUser(Long userId) {
        if (userId == null) return;
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "headUserId does not refer to an existing user");
        }
    }

    public static DepartmentResponse toResponse(DepartmentEntity entity) {
        return new DepartmentResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getHeadUserId(),
                Boolean.TRUE.equals(entity.getIsActive()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
