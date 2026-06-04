package com.peih68.leave.leaverequest.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.audit.AuditLogWriter;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.domain.ApprovalActionEntity;
import com.peih68.leave.leaverequest.domain.LeaveDayCalculator;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.ApprovalActionRepository;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leaverequest.web.dto.ApprovalActionResponse;
import com.peih68.leave.leaverequest.web.dto.ApprovalDecisionRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestResponse;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final LeaveBalanceService leaveBalanceService;
    private final HolidayService holidayService;
    private final LeaveDayCalculator dayCalculator;
    private final AuditLogWriter auditLogWriter;

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

    /** The requester edits their own PENDING request. Days are recomputed and re-validated. */
    @Transactional
    public LeaveRequestResponse update(Long id, LeaveRequestCreateRequest req, UserPrincipal principal) {
        LeaveRequestEntity r = requireRequest(id);
        if (!r.getUserId().equals(principal.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "you can only edit your own request");
        }
        requireStatus(r, LeaveStatus.PENDING);
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

        if (requestRepository.existsOverlapExcluding(
                r.getUserId(), ACTIVE_STATUSES, req.startDate(), req.endDate(), r.getId())) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "you already have a pending or approved request overlapping these dates");
        }

        if (Boolean.TRUE.equals(type.getRequiresBalance())) {
            BigDecimal remaining = balanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(r.getUserId(), type.getId(), req.startDate().getYear())
                    .map(LeaveBalanceEntity::remaining)
                    .orElse(BigDecimal.ZERO);
            if (remaining.compareTo(totalDays) < 0) {
                throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE,
                        "remaining balance %s is less than requested %s".formatted(remaining, totalDays));
            }
        }

        r.setLeaveTypeId(type.getId());
        r.setStartDate(req.startDate());
        r.setEndDate(req.endDate());
        r.setStartHalf(req.startHalf());
        r.setEndHalf(req.endHalf());
        r.setTotalDays(totalDays);
        r.setReason(req.reason());

        recordAction(r.getId(), principal.getId(), ApprovalAction.UPDATED,
                LeaveStatus.PENDING, LeaveStatus.PENDING, null);
        log.info("user {} edited leave request {} ({} days)", principal.getId(), r.getId(), totalDays);
        return toResponse(r);
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
        List<LeaveRequestEntity> rows = requestRepository
                .findByUserIdAndStartDateBetweenOrderByStartDateDesc(userId, from, to).stream()
                .filter(r -> status == null || r.getStatus() == status)
                .toList();
        return toResponses(rows);
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
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    /** Approve a pending request: hard-check + consume balance, then mark APPROVED. */
    @Transactional
    public LeaveRequestResponse approve(Long id, ApprovalDecisionRequest req, UserPrincipal actor) {
        LeaveRequestEntity r = requireRequest(id);
        requireStatus(r, LeaveStatus.PENDING);
        consumeBalanceIfNeeded(r, r.getTotalDays());
        return transition(r, ApprovalAction.APPROVED, LeaveStatus.APPROVED, actor, comment(req));
    }

    /** Reject a pending request. A comment is required. No balance change. */
    @Transactional
    public LeaveRequestResponse reject(Long id, ApprovalDecisionRequest req, UserPrincipal actor) {
        LeaveRequestEntity r = requireRequest(id);
        requireStatus(r, LeaveStatus.PENDING);
        String comment = comment(req);
        if (comment == null || comment.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "a comment is required when rejecting");
        }
        return transition(r, ApprovalAction.REJECTED, LeaveStatus.REJECTED, actor, comment);
    }

    /**
     * Cancel a request. The requester may cancel their own PENDING request; an APPROVED
     * request can only be cancelled by the manager/HR/ADMIN and restores the consumed balance.
     */
    @Transactional
    public LeaveRequestResponse cancel(Long id, ApprovalDecisionRequest req, UserPrincipal actor) {
        LeaveRequestEntity r = requireRequest(id);
        boolean privileged = actor.getRole() == Role.ADMIN || actor.getRole() == Role.HR
                || actor.getId().equals(r.getManagerId());
        boolean requester = actor.getId().equals(r.getUserId());

        switch (r.getStatus()) {
            case PENDING -> { /* requester or privileged (already checked at the controller) */ }
            case APPROVED -> {
                // Requirements §5.5: the requester may cancel an approved request only before
                // it starts; the manager/HR/ADMIN may always cancel (override). Cancelling
                // restores the consumed balance.
                if (!privileged) {
                    if (!requester) {
                        throw new ApiException(ErrorCode.FORBIDDEN, "you cannot cancel this request");
                    }
                    if (!LocalDate.now().isBefore(r.getStartDate())) {
                        throw new ApiException(ErrorCode.FORBIDDEN,
                                "an approved leave can only be cancelled by you before it starts;"
                                        + " ask your manager or HR");
                    }
                }
                consumeBalanceIfNeeded(r, r.getTotalDays().negate());
            }
            default -> throw new ApiException(ErrorCode.CONFLICT,
                    "request is %s and can no longer be cancelled".formatted(r.getStatus()));
        }
        return transition(r, ApprovalAction.CANCELLED, LeaveStatus.CANCELLED, actor, comment(req));
    }

    @Transactional(readOnly = true)
    public List<ApprovalActionResponse> history(Long requestId) {
        requireRequest(requestId);
        List<ApprovalActionEntity> actions =
                approvalActionRepository.findByLeaveRequestIdOrderByCreatedAtAsc(requestId);
        Map<Long, String> actorNames = userRepository
                .findAllById(actions.stream().map(ApprovalActionEntity::getActorId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        return actions.stream()
                .map(a -> new ApprovalActionResponse(
                        a.getId(), a.getAction(), a.getActorId(), actorNames.get(a.getActorId()),
                        a.getPreviousStatus(), a.getNewStatus(), a.getComment(), a.getCreatedAt()))
                .toList();
    }

    // --- helpers (shared with the approval workflow added in Part 3) ---

    private void consumeBalanceIfNeeded(LeaveRequestEntity r, BigDecimal delta) {
        LeaveTypeEntity type = leaveTypeRepository.findById(r.getLeaveTypeId()).orElse(null);
        if (type != null && Boolean.TRUE.equals(type.getRequiresBalance())) {
            leaveBalanceService.applyUsedDelta(r.getUserId(), r.getLeaveTypeId(), r.getStartDate().getYear(), delta);
        }
    }

    private LeaveRequestResponse transition(LeaveRequestEntity r, ApprovalAction action,
            LeaveStatus next, UserPrincipal actor, String comment) {
        LeaveStatus previous = r.getStatus();
        r.setStatus(next);
        recordAction(r.getId(), actor.getId(), action, previous, next, comment);
        auditLogWriter.record(actor.getId(), "LEAVE_REQUEST_" + action.name(), "leave_request", r.getId(),
                "{\"status\":\"" + previous + "\"}", "{\"status\":\"" + next + "\"}");
        log.info("request {} {} by user {} ({} -> {})", r.getId(), action, actor.getId(), previous, next);
        return toResponse(r);
    }

    private void requireStatus(LeaveRequestEntity r, LeaveStatus expected) {
        if (r.getStatus() != expected) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "request is %s, expected %s".formatted(r.getStatus(), expected));
        }
    }

    private static String comment(ApprovalDecisionRequest req) {
        return req == null ? null : req.comment();
    }

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

    /** Batch mapping for list endpoints — resolves user names and type codes once (no N+1). */
    private List<LeaveRequestResponse> toResponses(List<LeaveRequestEntity> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = rows.stream()
                .flatMap(r -> Stream.of(r.getUserId(), r.getManagerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        Map<Long, String> typeCodes = leaveTypeRepository
                .findAllById(rows.stream().map(LeaveRequestEntity::getLeaveTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(LeaveTypeEntity::getId, LeaveTypeEntity::getCode));

        return rows.stream()
                .map(r -> new LeaveRequestResponse(
                        r.getId(), r.getUserId(), userNames.get(r.getUserId()),
                        r.getLeaveTypeId(), typeCodes.get(r.getLeaveTypeId()),
                        r.getStartDate(), r.getEndDate(), r.getStartHalf(), r.getEndHalf(),
                        r.getTotalDays(), r.getReason(), r.getStatus(),
                        r.getManagerId(), r.getManagerId() == null ? null : userNames.get(r.getManagerId()),
                        r.getCreatedAt()))
                .toList();
    }
}
