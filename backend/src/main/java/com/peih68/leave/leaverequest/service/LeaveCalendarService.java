package com.peih68.leave.leaverequest.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.department.domain.DepartmentEntity;
import com.peih68.leave.department.repository.DepartmentRepository;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.web.dto.CalendarEntryResponse;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only team leave calendar (REQUIREMENTS §6). Returns leave occurrences overlapping
 * a date window, scoped to what the caller may see: an EMPLOYEE sees their whole
 * department; a MANAGER sees their department plus their direct reports; HR/ADMIN see
 * everyone, or one department via {@code departmentId} / one person via {@code userId}.
 * Only APPROVED leave is shown by default; {@code includePending} also surfaces pending
 * requests for planning. Optionally filtered by {@code leaveTypeId}.
 */
@Service
@RequiredArgsConstructor
public class LeaveCalendarService {

    private static final List<LeaveStatus> APPROVED_ONLY = List.of(LeaveStatus.APPROVED);
    private static final List<LeaveStatus> APPROVED_AND_PENDING =
            List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING);

    /** Guard rail: a calendar query may span at most one quarter. */
    private static final long MAX_RANGE_DAYS = 92;

    private final LeaveRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<CalendarEntryResponse> calendar(
            UserPrincipal principal,
            LocalDate from,
            LocalDate to,
            Long departmentId,
            Long leaveTypeId,
            Long userId,
            boolean includePending) {
        if (from == null || to == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "from and to are required");
        }
        if (to.isBefore(from)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "to must not be before from");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "calendar range must not exceed %d days".formatted(MAX_RANGE_DAYS));
        }

        List<LeaveStatus> statuses = includePending ? APPROVED_AND_PENDING : APPROVED_ONLY;
        List<LeaveRequestEntity> rows = fetchInScope(principal, from, to, departmentId, statuses);

        return toEntries(rows.stream()
                .filter(r -> leaveTypeId == null || leaveTypeId.equals(r.getLeaveTypeId()))
                .filter(r -> userId == null || userId.equals(r.getUserId()))
                .toList());
    }

    private List<LeaveRequestEntity> fetchInScope(
            UserPrincipal principal, LocalDate from, LocalDate to, Long departmentId,
            List<LeaveStatus> statuses) {
        Role role = principal.getRole();
        if (role == Role.ADMIN || role == Role.HR) {
            if (departmentId == null) {
                return requestRepository.findOverlapping(statuses, from, to);
            }
            return queryForUsers(deptMemberIds(departmentId), from, to, statuses);
        }

        // Employee/Manager: their whole department, plus a manager's direct reports.
        Set<Long> userIds = new HashSet<>();
        userIds.add(principal.getId());
        userRepository.findById(principal.getId())
                .map(UserEntity::getDepartmentId)
                .ifPresent(deptId -> userIds.addAll(deptMemberIds(deptId)));
        if (role == Role.MANAGER) {
            userRepository.findByManagerIdAndIsActiveTrue(principal.getId())
                    .forEach(u -> userIds.add(u.getId()));
        }
        return queryForUsers(userIds, from, to, statuses);
    }

    private Set<Long> deptMemberIds(Long departmentId) {
        return userRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
    }

    private List<LeaveRequestEntity> queryForUsers(
            Set<Long> userIds, LocalDate from, LocalDate to, List<LeaveStatus> statuses) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return requestRepository.findOverlappingForUsers(userIds, statuses, from, to);
    }

    /** Map entities to DTOs, batch-resolving user names and leave-type codes (no N+1). */
    private List<CalendarEntryResponse> toEntries(List<LeaveRequestEntity> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, UserEntity> users = userRepository
                .findAllById(rows.stream().map(LeaveRequestEntity::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
        Map<Long, String> typeCodes = leaveTypeRepository
                .findAllById(rows.stream().map(LeaveRequestEntity::getLeaveTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(LeaveTypeEntity::getId, LeaveTypeEntity::getCode));
        Map<Long, String> departmentNames = departmentRepository
                .findAllById(users.values().stream()
                        .map(UserEntity::getDepartmentId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, DepartmentEntity::getName));

        List<CalendarEntryResponse> out = new ArrayList<>(rows.size());
        for (LeaveRequestEntity r : rows) {
            UserEntity user = users.get(r.getUserId());
            Long departmentId = user == null ? null : user.getDepartmentId();
            out.add(new CalendarEntryResponse(
                    r.getId(), r.getUserId(), user == null ? null : user.getFullName(),
                    departmentId, departmentId == null ? null : departmentNames.get(departmentId),
                    typeCodes.get(r.getLeaveTypeId()),
                    r.getStartDate(), r.getEndDate(), r.getStartHalf(), r.getEndHalf(), r.getStatus()));
        }
        return out;
    }
}
