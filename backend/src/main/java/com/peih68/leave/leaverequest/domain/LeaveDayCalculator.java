package com.peih68.leave.leaverequest.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Computes the chargeable number of leave days for a request, excluding weekends
 * (Sat/Sun) and public holidays, with optional half-day handling at the boundaries.
 *
 * <p>Pure logic with no Spring/DB dependencies — holiday dates are passed in, so this
 * can be unit-tested directly with {@code new LeaveDayCalculator()}. The caller (service
 * layer) is responsible for fetching holidays and for validating sensible half combos.
 */
@Component
public class LeaveDayCalculator {

    private static final BigDecimal FULL = new BigDecimal("1.0");
    private static final BigDecimal HALF = new BigDecimal("0.5");

    /**
     * @param start     first day of leave (inclusive)
     * @param end       last day of leave (inclusive)
     * @param startHalf which portion of {@code start} is taken
     * @param endHalf   which portion of {@code end} is taken
     * @param holidays  set of public-holiday dates to exclude
     * @return chargeable days as a multiple of 0.5 (scale 1); {@code 0.0} if the whole
     *         range falls on weekends/holidays
     * @throws IllegalArgumentException if any argument is null or {@code end} is before {@code start}
     */
    public BigDecimal calculate(
            LocalDate start, LocalDate end, LeaveHalf startHalf, LeaveHalf endHalf, Set<LocalDate> holidays) {
        if (start == null || end == null || startHalf == null || endHalf == null || holidays == null) {
            throw new IllegalArgumentException("calculate() arguments must not be null");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end date must not be before start date");
        }

        // Single day: either a full working day (1.0) or a half day (0.5).
        if (start.equals(end)) {
            if (!isWorkingDay(start, holidays)) {
                return BigDecimal.ZERO.setScale(1);
            }
            boolean half = startHalf != LeaveHalf.FULL_DAY || endHalf != LeaveHalf.FULL_DAY;
            return half ? HALF : FULL;
        }

        // Multi-day: count each working day as 1.0, then shave 0.5 off each boundary
        // day that is itself a working day and taken as a half day.
        BigDecimal total = BigDecimal.ZERO.setScale(1);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isWorkingDay(d, holidays)) {
                total = total.add(FULL);
            }
        }
        if (startHalf != LeaveHalf.FULL_DAY && isWorkingDay(start, holidays)) {
            total = total.subtract(HALF);
        }
        if (endHalf != LeaveHalf.FULL_DAY && isWorkingDay(end, holidays)) {
            total = total.subtract(HALF);
        }
        return total;
    }

    private static boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidays.contains(date);
    }
}
