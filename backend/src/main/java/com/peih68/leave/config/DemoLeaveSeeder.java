package com.peih68.leave.config;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.holiday.service.HolidayService;
import com.peih68.leave.leavebalance.domain.LeaveBalanceEntity;
import com.peih68.leave.leavebalance.repository.LeaveBalanceRepository;
import com.peih68.leave.leavebalance.service.LeaveBalanceService;
import com.peih68.leave.leavebalance.web.dto.LeaveBalanceAdjustRequest;
import com.peih68.leave.leaverequest.domain.ApprovalAction;
import com.peih68.leave.leaverequest.domain.ApprovalActionEntity;
import com.peih68.leave.leaverequest.domain.LeaveDayCalculator;
import com.peih68.leave.leaverequest.domain.LeaveHalf;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.ApprovalActionRepository;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.leavetype.domain.LeaveTypeEntity;
import com.peih68.leave.leavetype.repository.LeaveTypeRepository;
import com.peih68.leave.user.domain.UserEntity;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds a natural-looking leave history for the dev profile: current-year balances,
 * ~70 requests spread over the past five months plus the next few weeks, every status,
 * half-days, and consistent approval_actions. Dates are all relative to today so the
 * data never goes stale. Invoked by {@link DemoDataInitializer} only on an empty DB.
 *
 * Invariants kept (same as the real business flow):
 * - total_days computed by LeaveDayCalculator with real holidays (never wrong on weekends);
 * - used_days updated through LeaveBalanceService.applyUsedDelta (never negative);
 * - one request per user-week slot, so no overlapping PENDING/APPROVED pairs;
 * - approval_actions follow the §5.4 state machine (CREATED then the final action).
 *
 * Constraint for the Playwright smoke (e2e/run_smoke.py): it submits at Monday +80 days
 * and cancels the FIRST row of "my requests" (sorted by start_date desc), so all seeded
 * future requests must start no later than today + 30 days.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoLeaveSeeder {

    private final LeaveRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveBalanceService balanceService;
    private final HolidayService holidayService;
    private final LeaveDayCalculator dayCalculator;

    private static final String[] REASONS = {
            "Về quê thăm gia đình", "Khám sức khỏe định kỳ", "Việc gia đình",
            "Du lịch cùng gia đình", "Con ốm, cần chăm sóc", "Đi đám cưới người thân",
            "Giải quyết giấy tờ cá nhân", "Nghỉ ngơi phục hồi sức khỏe",
            "Sửa chữa nhà cửa", "Đưa người thân đi viện",
    };

    private Map<String, LeaveTypeEntity> typesByCode;

    /** users = everyone with a manager (the admin is excluded by the caller). */
    public void seed(List<UserEntity> users, UserEntity admin) {
        typesByCode = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(LeaveTypeEntity::getCode, t -> t));

        int year = LocalDate.now().getYear();
        // Past weeks may fall into the previous year when run early in a year.
        balanceService.bulkInitializeYear(year - 1);
        int rows = balanceService.bulkInitializeYear(year);
        log.info("Seeded {} leave balance rows for {}", rows, year);

        int count = 0;
        for (int i = 0; i < users.size(); i++) {
            count += seedHistoryFor(users.get(i), i);
        }
        count += seedOnLeaveNow(users);
        count += seedUpcoming(users);
        seedAdjustments(users, admin);
        log.info("Seeded {} demo leave requests", count);
    }

    /** Three past requests per user in disjoint week slots; status varies by index. */
    private int seedHistoryFor(UserEntity u, int i) {
        int created = 0;
        int[] weeksBack = {2 + (i % 3), 7 + (i % 4), 13 + (i % 5)};
        String[] typeCycle = {"ANNUAL", "SICK", "PERSONAL", "ANNUAL", "UNPAID", "ANNUAL", "SICK"};
        for (int k = 0; k < weeksBack.length; k++) {
            int idx = i * 3 + k;
            LocalDate start = LocalDate.now().minusWeeks(weeksBack[k])
                    .with(DayOfWeek.MONDAY).plusDays(idx % 3);
            LocalDate end = start.plusDays(idx % 3 == 1 ? 2 : idx % 2); // 1-3 weekdays, same week
            String typeCode = typeCycle[idx % typeCycle.length];
            // PERSONAL quota is only 3 days/year — keep those requests to a single day.
            if (typeCode.equals("PERSONAL")) {
                end = start;
            }
            LeaveHalf startHalf = idx % 5 == 4 && !start.equals(end) ? LeaveHalf.AFTERNOON : LeaveHalf.FULL_DAY;
            LeaveStatus finalStatus = switch (idx % 7) {
                case 2 -> LeaveStatus.REJECTED;
                case 4 -> LeaveStatus.CANCELLED;          // cancelled while pending
                case 6 -> LeaveStatus.CANCELLED;          // cancelled after approval (idx%7==6)
                default -> LeaveStatus.APPROVED;
            };
            boolean cancelledAfterApprove = idx % 7 == 6;
            if (request(u, typeCode, start, end, startHalf, LeaveHalf.FULL_DAY,
                    REASONS[idx % REASONS.length], finalStatus, cancelledAfterApprove) != null) {
                created++;
            }
        }
        return created;
    }

    /** A few APPROVED requests covering today (one per department), so dashboards are alive. */
    private int seedOnLeaveNow(List<UserEntity> users) {
        int created = 0;
        int[] picks = {0, 4, 14}; // HR Demo, an ENG employee, a SALES employee
        for (int p = 0; p < picks.length && picks[p] < users.size(); p++) {
            UserEntity u = users.get(picks[p]);
            LocalDate start = LocalDate.now().minusDays(1);
            LocalDate end = LocalDate.now().plusDays(1);
            if (request(u, "ANNUAL", start, end, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY,
                    "Nghỉ phép năm theo kế hoạch", LeaveStatus.APPROVED, false) != null) {
                created++;
            }
        }
        return created;
    }

    /** Future requests within +30 days: some APPROVED (calendar) and some PENDING (inboxes). */
    private int seedUpcoming(List<UserEntity> users) {
        int created = 0;
        for (int i = 0; i < users.size(); i++) {
            UserEntity u = users.get(i);
            if (i % 4 == 1) { // PENDING for the approver inboxes
                LocalDate start = LocalDate.now().plusWeeks(1 + i % 3).with(DayOfWeek.WEDNESDAY);
                if (request(u, i % 2 == 0 ? "ANNUAL" : "PERSONAL", start, start.plusDays(i % 2),
                        LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY,
                        REASONS[(i + 3) % REASONS.length], LeaveStatus.PENDING, false) != null) {
                    created++;
                }
            } else if (i % 4 == 3) { // APPROVED upcoming
                LocalDate start = LocalDate.now().plusWeeks(2 + i % 2).with(DayOfWeek.MONDAY);
                if (request(u, "ANNUAL", start, start.plusDays(1 + i % 2),
                        LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY,
                        REASONS[(i + 5) % REASONS.length], LeaveStatus.APPROVED, false) != null) {
                    created++;
                }
            }
        }
        return created;
    }

    /** A couple of manual balance adjustments so the admin screens and audit log have data. */
    private void seedAdjustments(List<UserEntity> users, UserEntity admin) {
        Long annualId = typesByCode.get("ANNUAL").getId();
        int year = LocalDate.now().getYear();
        String[] reasons = {"Thưởng thâm niên", "Chuyển phép tồn theo thỏa thuận"};
        for (int i = 0; i < 2 && i * 5 < users.size(); i++) {
            UserEntity u = users.get(i * 5);
            String reason = reasons[i];
            balanceRepository.findByUserIdAndLeaveTypeIdAndYear(u.getId(), annualId, year)
                    .map(LeaveBalanceEntity::getId)
                    .ifPresent(id -> balanceService.adjust(id,
                            new LeaveBalanceAdjustRequest(BigDecimal.ONE, reason),
                            UserPrincipal.from(admin)));
        }
    }

    /**
     * Creates one request with consistent days, actions and balance. Returns null (skips)
     * when the range has no working days or the balance cannot cover it.
     */
    private LeaveRequestEntity request(UserEntity u, String typeCode, LocalDate start, LocalDate end,
            LeaveHalf startHalf, LeaveHalf endHalf, String reason,
            LeaveStatus finalStatus, boolean cancelledAfterApprove) {
        LeaveTypeEntity type = typesByCode.get(typeCode);
        Set<LocalDate> holidays = holidayService.holidayDatesBetween(start, end);
        BigDecimal totalDays = dayCalculator.calculate(start, end, startHalf, endHalf, holidays);
        if (totalDays.signum() <= 0) {
            return null; // all weekend/holiday — skip this slot
        }

        // Consume balance exactly like approve() would; skip the request if quota is exhausted.
        boolean consumes = finalStatus == LeaveStatus.APPROVED
                && Boolean.TRUE.equals(type.getRequiresBalance());
        if (consumes) {
            BigDecimal remaining = balanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(u.getId(), type.getId(), start.getYear())
                    .map(LeaveBalanceEntity::remaining)
                    .orElse(BigDecimal.ZERO);
            if (remaining.compareTo(totalDays) < 0) {
                return null;
            }
            balanceService.applyUsedDelta(u.getId(), type.getId(), start.getYear(), totalDays);
        }

        LeaveRequestEntity r = requestRepository.save(LeaveRequestEntity.builder()
                .userId(u.getId()).leaveTypeId(type.getId())
                .startDate(start).endDate(end).startHalf(startHalf).endHalf(endHalf)
                .totalDays(totalDays).reason(reason).status(finalStatus)
                .managerId(u.getManagerId())
                .build());

        action(r, u.getId(), ApprovalAction.CREATED, null, LeaveStatus.PENDING, null);
        switch (finalStatus) {
            case APPROVED -> action(r, u.getManagerId(), ApprovalAction.APPROVED,
                    LeaveStatus.PENDING, LeaveStatus.APPROVED, null);
            case REJECTED -> action(r, u.getManagerId(), ApprovalAction.REJECTED,
                    LeaveStatus.PENDING, LeaveStatus.REJECTED, "Trùng lịch cao điểm, vui lòng chọn tuần khác");
            case CANCELLED -> {
                if (cancelledAfterApprove) {
                    // Approved then cancelled: balance was consumed and restored — net zero.
                    action(r, u.getManagerId(), ApprovalAction.APPROVED,
                            LeaveStatus.PENDING, LeaveStatus.APPROVED, null);
                    action(r, u.getId(), ApprovalAction.CANCELLED,
                            LeaveStatus.APPROVED, LeaveStatus.CANCELLED, "Thay đổi kế hoạch cá nhân");
                } else {
                    action(r, u.getId(), ApprovalAction.CANCELLED,
                            LeaveStatus.PENDING, LeaveStatus.CANCELLED, "Không còn nhu cầu nghỉ");
                }
            }
            default -> { /* PENDING: only CREATED */ }
        }
        return r;
    }

    private void action(LeaveRequestEntity r, Long actorId, ApprovalAction action,
            LeaveStatus previous, LeaveStatus next, String comment) {
        actionRepository.save(ApprovalActionEntity.builder()
                .leaveRequestId(r.getId()).actorId(actorId).action(action)
                .previousStatus(previous).newStatus(next).comment(comment)
                .build());
    }
}
