package com.peih68.leave.leaverequest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure unit test — no Spring context. 2026-01-05..09 is a Mon–Fri work week with no holidays. */
class LeaveDayCalculatorTest {

    private final LeaveDayCalculator calc = new LeaveDayCalculator();

    private static final LocalDate MON = LocalDate.of(2026, 1, 5);
    private static final LocalDate TUE = LocalDate.of(2026, 1, 6);
    private static final LocalDate WED = LocalDate.of(2026, 1, 7);
    private static final LocalDate FRI = LocalDate.of(2026, 1, 9);
    private static final LocalDate NEXT_MON = LocalDate.of(2026, 1, 12);
    private static final LocalDate SAT = LocalDate.of(2026, 1, 3);
    private static final LocalDate SUN = LocalDate.of(2026, 1, 4);

    @Test
    void fullWorkWeekIsFiveDays() {
        assertThat(calc.calculate(MON, FRI, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, Set.of()))
                .isEqualByComparingTo("5.0");
    }

    @Test
    void bridgingWeekendExcludesSaturdaySunday() {
        // Mon 05 .. Mon 12 → 05,06,07,08,09 + 12 = 6 working days (skip Sat 10, Sun 11)
        assertThat(calc.calculate(MON, NEXT_MON, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, Set.of()))
                .isEqualByComparingTo("6.0");
    }

    @Test
    void holidayInRangeIsExcluded() {
        // Mon 05 .. Fri 09 with Wed 07 as a holiday → 4 working days
        assertThat(calc.calculate(MON, FRI, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, Set.of(WED)))
                .isEqualByComparingTo("4.0");
    }

    @Test
    void singleFullDay() {
        assertThat(calc.calculate(MON, MON, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, Set.of()))
                .isEqualByComparingTo("1.0");
    }

    @Test
    void singleMorningHalfDay() {
        assertThat(calc.calculate(MON, MON, LeaveHalf.MORNING, LeaveHalf.MORNING, Set.of()))
                .isEqualByComparingTo("0.5");
    }

    @Test
    void singleAfternoonHalfDay() {
        assertThat(calc.calculate(MON, MON, LeaveHalf.AFTERNOON, LeaveHalf.AFTERNOON, Set.of()))
                .isEqualByComparingTo("0.5");
    }

    @Test
    void multiDayStartingAfternoon() {
        // Mon..Wed (3 working days), start afternoon → 2.5
        assertThat(calc.calculate(MON, WED, LeaveHalf.AFTERNOON, LeaveHalf.FULL_DAY, Set.of()))
                .isEqualByComparingTo("2.5");
    }

    @Test
    void multiDayEndingMorning() {
        // Mon..Wed (3), end morning → 2.5
        assertThat(calc.calculate(MON, WED, LeaveHalf.FULL_DAY, LeaveHalf.MORNING, Set.of()))
                .isEqualByComparingTo("2.5");
    }

    @Test
    void multiDayBothBoundariesHalf() {
        // Mon afternoon .. Wed morning → 3 - 0.5 - 0.5 = 2.0
        assertThat(calc.calculate(MON, WED, LeaveHalf.AFTERNOON, LeaveHalf.MORNING, Set.of()))
                .isEqualByComparingTo("2.0");
    }

    @Test
    void entirelyWeekendIsZero() {
        assertThat(calc.calculate(SAT, SUN, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, Set.of()))
                .isEqualByComparingTo("0.0");
    }

    @Test
    void halfOnWeekendBoundaryIsNotSubtracted() {
        // Sat 03 .. Tue 06, start afternoon: only Mon 05 + Tue 06 are working days = 2.0;
        // Sat is not a working day so no 0.5 shave applies.
        assertThat(calc.calculate(SAT, TUE, LeaveHalf.AFTERNOON, LeaveHalf.FULL_DAY, Set.of()))
                .isEqualByComparingTo("2.0");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() ->
                        calc.calculate(FRI, MON, LeaveHalf.FULL_DAY, LeaveHalf.FULL_DAY, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
