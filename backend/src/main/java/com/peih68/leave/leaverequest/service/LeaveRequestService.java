package com.peih68.leave.leaverequest.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.domain.ApprovalActionEntity;
import com.peih68.leave.leaverequest.domain.LeaveDayCalculator;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.ApprovalActionRepository;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestResponse;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final List<LeaveStatus> ACTIVE_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository requestRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final HolidayService holidayService;
    private final LeaveDayCalculator dayCalculator;

    /** An employee submits a leave request for themselves. */
    @Transactional
    public LeaveRequestResponse submit(LeaveRequestCreateRequest req, UserPrincipal principal) {
        UserEntity user = requireUser(principal.getId());
        LeaveTypeEntity type = requireActiveLeaveType(req.leaveTypeId());

        if (req.endDate().isBefore(req.startDate())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "endDate must not be before startDate");
        }
        if (req.startDate().equals(req.endDate()) && req.startHalf() != req.endHalf()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "single-day request must use the same start and end half");
        }

        Set<LocalDate> holidays = holidayService.holidayDatesBetween(req.startDate(), req.endDate());
        BigDecimal totalDays =
                dayCalculator.calculate(req.startDate(), req.endDate(), req.startHalf(), req.endHalf(), holidays);
        if (totalDays.signum() <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "leave range contains no working days (all weekend/holiday)");
        }

        Long managerId = user.getManagerId();
        if (managerId == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "you have no assigned manager to approve this request");
        }

        if (requestRepository.existsOverlap(user.getId(), ACTIVE_STATUSES, req.startDate(), req.endDate())) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "you already have a pending or approved request overlapping these dates");
        }

        // Soft check at submission; a hard re-check happens at approval time.
        if (Boolean.TRUE.equals(type.getRequiresBalance())) {
            BigDecimal remaining = balanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(user.getId(), type.getId(), req.startDate().getYear())
                    .map(LeaveBalanceEntity::remaining)
                    .orElse(BigDecimal.ZERO);
            if (remaining.compareTo(totalDays) < 0) {
                throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE,
                        "remaining balance %s is less than requested %s".formatted(remaining, totalDays));
            }
        }

        LeaveRequestEntity entity = requestRepository.save(LeaveRequestEntity.builder()
                .userId(user.getId())
                .leaveTypeId(type.getId())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .startHalf(req.startHalf())
                .endHalf(req.endHalf())
                .totalDays(totalDays)
                .reason(req.reason())
                .status(LeaveStatus.PENDING)
                .managerId(managerId)
                .build());

        recordAction(entity.getId(), user.getId(), ApprovalAction.CREATED, null, LeaveStatus.PENDING, null);
        log.info("user {} submitted leave request {} ({} days)", user.getId(), entity.getId(), totalDays);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse findById(Long id) {
        return toResponse(requireRequest(id));
    }

    /** A single user's requests, optionally filtered by year and status. */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> listByUser(Long userId, Integer year, LeaveStatus status) {
        requireUser(userId);
        LocalDate from = year != null ? LocalDate.of(year, 1, 1) : LocalDate.of(1900, 1, 1);
        LocalDate to = year != null ? LocalDate.of(year, 12, 31) : LocalDate.of(9999, 12, 31);
        return requestRepository
                .findByUserIdAndStartDateBetweenOrderByStartDateDesc(userId, from, to).stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(this::toResponse)
                .toList();
    }

    /** Approver inbox: a MANAGER sees their team's requests; HR/ADMIN see everyone's. */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> listForApprover(UserPrincipal principal, LeaveStatus status, Pageable pageable) {
        boolean privileged = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        Page<LeaveRequestEntity> page;
        if (privileged) {
            page = status != null
                    ? requestRepository.findByStatusOrderByStartDateAsc(status, pageable)
                    : requestRepository.findAllByOrderByStartDateAsc(pageable);
        } else {
            page = status != null
                    ? requestRepository.findByManagerIdAndStatusOrderByStartDateAsc(principal.getId(), status, pageable)
                    : requestRepository.findByManagerIdOrderByStartDateAsc(principal.getId(), pageable);
        }
        return page.map(this::toResponse);
    }

    // --- helpers (shared with the approval workflow added in Part 3) ---

    void recordAction(Long requestId, Long actorId, ApprovalAction action,
            LeaveStatus previous, LeaveStatus next, String comment) {
        approvalActionRepository.save(ApprovalActionEntity.builder()
                .leaveRequestId(requestId)
                .actorId(actorId)
                .action(action)
                .previousStatus(previous)
                .newStatus(next)
                .comment(comment)
                .build());
    }

    LeaveRequestEntity requireRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Leave request not found: " + id));
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "userId does not exist: " + id));
    }

    private LeaveTypeEntity requireActiveLeaveType(Long id) {
        LeaveTypeEntity type = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "leaveTypeId does not exist: " + id));
        if (!Boolean.TRUE.equals(type.getIsActive())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "leave type is not active: " + id);
        }
        return type;
    }

    private LeaveRequestResponse toResponse(LeaveRequestEntity r) {
        String userName = userRepository.findById(r.getUserId()).map(UserEntity::getFullName).orElse(null);
        String typeCode = leaveTypeRepository.findById(r.getLeaveTypeId())
                .map(LeaveTypeEntity::getCode).orElse(null);
        String managerName = r.getManagerId() == null ? null
                : userRepository.findById(r.getManagerId()).map(UserEntity::getFullName).orElse(null);
        return new LeaveRequestResponse(
                r.getId(), r.getUserId(), userName,
                r.getLeaveTypeId(), typeCode,
                r.getStartDate(), r.getEndDate(), r.getStartHalf(), r.getEndHalf(),
                r.getTotalDays(), r.getReason(), r.getStatus(),
                r.getManagerId(), managerName, r.getCreatedAt());
    }
}
