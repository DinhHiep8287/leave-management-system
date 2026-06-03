package com.peih68.leave.leavebalance.service;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.audit.AuditLogWriter;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceAdjustRequest;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceResponse;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceUpsertRequest;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final AuditLogWriter auditLogWriter;

    /** Set / overwrite the quota (total_days) for one user-type-year. */
    @Transactional
    public LeaveBalanceResponse upsert(LeaveBalanceUpsertRequest req) {
        UserEntity user = requireUser(req.userId());
        LeaveTypeEntity type = requireLeaveType(req.leaveTypeId());

        LeaveBalanceEntity entity = balanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(req.userId(), req.leaveTypeId(), req.year())
                .orElseGet(() -> LeaveBalanceEntity.builder()
                        .userId(req.userId())
                        .leaveTypeId(req.leaveTypeId())
                        .year(req.year())
                        .usedDays(BigDecimal.ZERO)
                        .adjustedDays(BigDecimal.ZERO)
                        .build());
        entity.setTotalDays(req.totalDays());
        LeaveBalanceEntity saved = balanceRepository.save(entity);
        return toResponse(saved, user, type);
    }

    /**
     * Create missing balance rows for every active leave type that requires a
     * balance, for all active users. Idempotent — existing rows are left intact.
     */
    @Transactional
    public int bulkInitializeYear(int year) {
        List<LeaveTypeEntity> types = leaveTypeRepository.findByIsActiveTrueAndRequiresBalanceTrueOrderByCodeAsc();
        List<UserEntity> users = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .toList();
        int created = 0;
        for (UserEntity user : users) {
            for (LeaveTypeEntity type : types) {
                if (!balanceRepository.existsByUserIdAndLeaveTypeIdAndYear(user.getId(), type.getId(), year)) {
                    balanceRepository.save(LeaveBalanceEntity.builder()
                            .userId(user.getId())
                            .leaveTypeId(type.getId())
                            .year(year)
                            .totalDays(type.getDefaultQuotaDays())
                            .usedDays(BigDecimal.ZERO)
                            .adjustedDays(BigDecimal.ZERO)
                            .build());
                    created++;
                }
            }
        }
        log.info("bulkInitializeYear({}) created {} balance rows", year, created);
        return created;
    }

    @Transactional
    public LeaveBalanceResponse adjust(Long id, LeaveBalanceAdjustRequest req, UserPrincipal actor) {
        LeaveBalanceEntity entity = balanceRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Leave balance not found: " + id));
        BigDecimal before = entity.getAdjustedDays();
        BigDecimal after = before.add(req.adjustedDaysDelta());
        if (entity.getTotalDays().add(after).subtract(entity.getUsedDays()).compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Adjustment would make remaining balance negative");
        }
        entity.setAdjustedDays(after);

        auditLogWriter.record(
                actor == null ? null : actor.getId(),
                "LEAVE_BALANCE_ADJUST",
                "leave_balance",
                entity.getId(),
                toJson(Map.of("adjustedDays", before)),
                toJson(linkedMap(
                        "adjustedDays", after,
                        "delta", req.adjustedDaysDelta(),
                        "reason", req.reason())));

        UserEntity user = requireUser(entity.getUserId());
        LeaveTypeEntity type = requireLeaveType(entity.getLeaveTypeId());
        return toResponse(entity, user, type);
    }

    /**
     * Apply a delta to {@code used_days} for one user-type-year (positive = consume on
     * approval, negative = restore on cancellation). Guards against negative used days and
     * against remaining dropping below zero. Used by the leave-request approval workflow.
     */
    @Transactional
    public void applyUsedDelta(Long userId, Long leaveTypeId, int year, BigDecimal delta) {
        LeaveBalanceEntity entity = balanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, year)
                .orElseThrow(() -> new ApiException(ErrorCode.INSUFFICIENT_BALANCE,
                        "no leave balance for user %d type %d year %d".formatted(userId, leaveTypeId, year)));
        BigDecimal newUsed = entity.getUsedDays().add(delta);
        if (newUsed.signum() < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "used days cannot drop below zero");
        }
        if (entity.getTotalDays().add(entity.getAdjustedDays()).subtract(newUsed).signum() < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE,
                    "insufficient balance to consume %s days".formatted(delta));
        }
        entity.setUsedDays(newUsed);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> findByUser(Long userId, int year) {
        UserEntity user = requireUser(userId);
        List<LeaveBalanceEntity> rows = balanceRepository.findByUserIdAndYearOrderByLeaveTypeId(userId, year);
        return rows.stream()
                .map(b -> toResponse(b, user, requireLeaveType(b.getLeaveTypeId())))
                .toList();
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "userId does not exist: " + id));
    }

    private LeaveTypeEntity requireLeaveType(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "leaveTypeId does not exist: " + id));
    }

    private static LeaveBalanceResponse toResponse(LeaveBalanceEntity b, UserEntity user, LeaveTypeEntity type) {
        return new LeaveBalanceResponse(
                b.getId(), b.getUserId(), user.getFullName(),
                b.getLeaveTypeId(), type.getCode(), b.getYear(),
                b.getTotalDays(), b.getUsedDays(), b.getAdjustedDays(), b.remaining());
    }

    private static Map<String, Object> linkedMap(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append('"').append(String.valueOf(v).replace("\"", "\\\"")).append('"');
            }
        }
        return sb.append("}").toString();
    }
}
