package com.peih68.leave.report.service;

import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds CSV exports for HR/ADMIN: leave requests in a period and balances for a year. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final List<LeaveStatus> ALL_STATUSES = List.of(LeaveStatus.values());

    private final LeaveRequestRepository requestRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    /** Leave requests overlapping [from, to], optionally filtered by status and department. */
    @Transactional(readOnly = true)
    public String leaveRequestsCsv(LocalDate from, LocalDate to, LeaveStatus status, Long departmentId) {
        List<LeaveStatus> statuses = status != null ? List.of(status) : ALL_STATUSES;
        List<LeaveRequestEntity> rows = requestRepository.findOverlapping(statuses, from, to);

        if (departmentId != null) {
            Set<Long> deptUserIds = userRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                    .map(UserEntity::getId)
                    .collect(Collectors.toSet());
            rows = rows.stream().filter(r -> deptUserIds.contains(r.getUserId())).toList();
        }

        Map<Long, UserEntity> users = usersById(rows.stream()
                .flatMap(r -> Stream.of(r.getUserId(), r.getManagerId()))
                .filter(Objects::nonNull).distinct().toList());
        Map<Long, String> typeCodes = typeCodesById(
                rows.stream().map(LeaveRequestEntity::getLeaveTypeId).distinct().toList());

        CsvWriter csv = new CsvWriter(
                "id", "employeeCode", "userFullName", "leaveTypeCode", "startDate", "endDate",
                "totalDays", "status", "managerName", "reason", "createdAt");
        for (LeaveRequestEntity r : rows) {
            UserEntity u = users.get(r.getUserId());
            UserEntity mgr = r.getManagerId() == null ? null : users.get(r.getManagerId());
            csv.row(
                    r.getId(),
                    u == null ? null : u.getEmployeeCode(),
                    u == null ? null : u.getFullName(),
                    typeCodes.get(r.getLeaveTypeId()),
                    r.getStartDate(), r.getEndDate(), r.getTotalDays(), r.getStatus(),
                    mgr == null ? null : mgr.getFullName(),
                    r.getReason(), r.getCreatedAt());
        }
        return csv.build();
    }

    /** All leave balances for a year. */
    @Transactional(readOnly = true)
    public String leaveBalancesCsv(int year) {
        List<LeaveBalanceEntity> rows = balanceRepository.findByYearOrderByUserIdAscLeaveTypeIdAsc(year);

        Map<Long, UserEntity> users = usersById(
                rows.stream().map(LeaveBalanceEntity::getUserId).distinct().toList());
        Map<Long, String> typeCodes = typeCodesById(
                rows.stream().map(LeaveBalanceEntity::getLeaveTypeId).distinct().toList());

        CsvWriter csv = new CsvWriter(
                "userFullName", "employeeCode", "leaveTypeCode", "year",
                "totalDays", "usedDays", "adjustedDays", "remainingDays");
        for (LeaveBalanceEntity b : rows) {
            UserEntity u = users.get(b.getUserId());
            csv.row(
                    u == null ? null : u.getFullName(),
                    u == null ? null : u.getEmployeeCode(),
                    typeCodes.get(b.getLeaveTypeId()),
                    b.getYear(), b.getTotalDays(), b.getUsedDays(), b.getAdjustedDays(), b.remaining());
        }
        return csv.build();
    }

    /**
     * Total approved leave days per period (month or quarter) and leave type for a year
     * (REQUIREMENTS §11). Days are attributed to the period of the request's start date.
     */
    @Transactional(readOnly = true)
    public String leaveSummaryCsv(int year, String groupBy) {
        boolean quarter = "quarter".equalsIgnoreCase(groupBy);
        List<LeaveRequestEntity> rows = requestRepository.findByStatusAndStartDateBetween(
                LeaveStatus.APPROVED, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        Map<Long, String> typeCodes = typeCodesById(
                rows.stream().map(LeaveRequestEntity::getLeaveTypeId).distinct().toList());

        // Keyed "period|typeCode" so the TreeMap sorts by period then type.
        Map<String, BigDecimal> sums = new TreeMap<>();
        for (LeaveRequestEntity r : rows) {
            int month = r.getStartDate().getMonthValue();
            String period = quarter ? "Q" + ((month - 1) / 3 + 1) : String.format("%02d", month);
            String code = typeCodes.getOrDefault(r.getLeaveTypeId(), String.valueOf(r.getLeaveTypeId()));
            sums.merge(period + "|" + code, r.getTotalDays(), BigDecimal::add);
        }

        CsvWriter csv = new CsvWriter(quarter ? "quarter" : "month", "leaveTypeCode", "totalDays");
        for (Map.Entry<String, BigDecimal> e : sums.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            csv.row(parts[0], parts[1], e.getValue());
        }
        return csv.build();
    }

    private Map<Long, UserEntity> usersById(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private Map<Long, String> typeCodesById(List<Long> ids) {
        return leaveTypeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LeaveTypeEntity::getId, LeaveTypeEntity::getCode));
    }
}
