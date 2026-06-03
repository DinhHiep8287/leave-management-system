package com.peih68.leave.leaverequest.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
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
 * Read-only team leave calendar. Returns the leave occurrences overlapping a date
 * window, scoped to what the caller may see: an EMPLOYEE sees only their own; a
 * MANAGER sees their direct reports plus themselves; HR/ADMIN see everyone (or one
 * department when {@code departmentId} is supplied).
 */
@Service
@RequiredArgsConstructor
public class LeaveCalendarService {

    /** Both APPROVED and PENDING are shown so planners can see tentative leave. */
    private static final List<LeaveStatus> CALENDAR_STATUSES =
            List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING);

    /** Guard rail: a calendar query may span at most one quarter. */
    private static final long MAX_RANGE_DAYS = 92;

    private final LeaveRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional(readOnly = true)
    public List<CalendarEntryResponse> calendar(
            UserPrincipal principal, LocalDate from, LocalDate to, Long departmentId) {
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

        List<LeaveRequestEntity> rows = fetchInScope(principal, from, to, departmentId);
        return toEntries(rows);
    }

    private List<LeaveRequestEntity> fetchInScope(
            UserPrincipal principal, LocalDate from, LocalDate to, Long departmentId) {
        Role role = principal.getRole();
        if (role == Role.ADMIN || role == Role.HR) {
            if (departmentId == null) {
                return requestRepository.findOverlapping(CALENDAR_STATUSES, from, to);
            }
            return queryForUsers(deptMemberIds(departmentId), from, to);
        }

        Set<Long> userIds = new HashSet<>();
        userIds.add(principal.getId());
        if (role == Role.MANAGER) {
            userRepository.findByManagerIdAndIsActiveTrue(principal.getId())
                    .forEach(u -> userIds.add(u.getId()));
        }
        return queryForUsers(userIds, from, to);
    }

    private Set<Long> deptMemberIds(Long departmentId) {
        return userRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
    }

    private List<LeaveRequestEntity> queryForUsers(
            Set<Long> userIds, LocalDate from, LocalDate to) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return requestRepository.findOverlappingForUsers(userIds, CALENDAR_STATUSES, from, to);
    }

    /** Map entities to DTOs, batch-resolving user names and leave-type codes (no N+1). */
    private List<CalendarEntryResponse> toEntries(List<LeaveRequestEntity> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, String> userNames = userRepository
                .findAllById(rows.stream().map(LeaveRequestEntity::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        Map<Long, String> typeCodes = leaveTypeRepository
                .findAllById(rows.stream().map(LeaveRequestEntity::getLeaveTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(LeaveTypeEntity::getId, LeaveTypeEntity::getCode));

        List<CalendarEntryResponse> out = new ArrayList<>(rows.size());
        for (LeaveRequestEntity r : rows) {
            out.add(new CalendarEntryResponse(
                    r.getId(), r.getUserId(), userNames.get(r.getUserId()),
                    typeCodes.get(r.getLeaveTypeId()),
                    r.getStartDate(), r.getEndDate(), r.getStartHalf(), r.getEndHalf(), r.getStatus()));
        }
        return out;
    }
}
