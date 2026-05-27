package com.peih68.leave.leavetype.service;

import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.leavetype.web.dto.LeaveTypeRequest;
import com.peih68.leave.leavetype.web.dto.LeaveTypeResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final LeaveTypeRepository leaveTypeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public LeaveTypeResponse create(LeaveTypeRequest req) {
        String code = req.code().toUpperCase();
        if (leaveTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.CONFLICT, "Leave type code already exists: " + code);
        }
        validateQuotaStep(req.defaultQuotaDays());
        LeaveTypeEntity entity = LeaveTypeEntity.builder()
                .code(code)
                .name(req.name().trim())
                .description(trimToNull(req.description()))
                .defaultQuotaDays(req.defaultQuotaDays())
                .requiresBalance(req.requiresBalance() == null ? Boolean.TRUE : req.requiresBalance())
                .isActive(req.isActive() == null ? Boolean.TRUE : req.isActive())
                .build();
        return toResponse(leaveTypeRepository.save(entity));
    }

    @Transactional
    public LeaveTypeResponse update(Long id, LeaveTypeRequest req) {
        LeaveTypeEntity entity = findOrThrow(id);
        String code = req.code().toUpperCase();
        if (!entity.getCode().equalsIgnoreCase(code) && leaveTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.CONFLICT, "Leave type code already exists: " + code);
        }
        validateQuotaStep(req.defaultQuotaDays());
        entity.setCode(code);
        entity.setName(req.name().trim());
        entity.setDescription(trimToNull(req.description()));
        entity.setDefaultQuotaDays(req.defaultQuotaDays());
        if (req.requiresBalance() != null) entity.setRequiresBalance(req.requiresBalance());
        if (req.isActive() != null) entity.setIsActive(req.isActive());
        return toResponse(entity);
    }

    @Transactional
    public void softDelete(Long id) {
        LeaveTypeEntity entity = findOrThrow(id);
        entity.setIsActive(false);
    }

    @Transactional
    public void hardDelete(Long id) {
        LeaveTypeEntity entity = findOrThrow(id);
        long balances = count("SELECT COUNT(*) FROM leave_balances WHERE leave_type_id = ?", id);
        long requests = count("SELECT COUNT(*) FROM leave_requests WHERE leave_type_id = ?", id);
        if (balances > 0 || requests > 0) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "Leave type is referenced by balances/requests; deactivate it instead of deleting");
        }
        leaveTypeRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public LeaveTypeResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> list(boolean activeOnly) {
        List<LeaveTypeEntity> entities = activeOnly
                ? leaveTypeRepository.findByIsActiveTrueOrderByCodeAsc()
                : leaveTypeRepository.findAllByOrderByCodeAsc();
        return entities.stream().map(LeaveTypeService::toResponse).toList();
    }

    private LeaveTypeEntity findOrThrow(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Leave type not found: " + id));
    }

    private void validateQuotaStep(BigDecimal quota) {
        // Must be a multiple of 0.5 (half-day granularity)
        if (quota.remainder(HALF).compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "defaultQuotaDays must be a multiple of 0.5");
        }
    }

    private long count(String sql, Long id) {
        Long c = jdbcTemplate.queryForObject(sql, Long.class, id);
        return c == null ? 0 : c;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static LeaveTypeResponse toResponse(LeaveTypeEntity e) {
        return new LeaveTypeResponse(
                e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getDefaultQuotaDays(),
                Boolean.TRUE.equals(e.getRequiresBalance()),
                Boolean.TRUE.equals(e.getIsActive()),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
