import type { LeaveHalf } from "@/features/leave-requests/types";

/**
 * Client-side preview of the backend LeaveDayCalculator: counts working days in
 * [start, end] (ISO yyyy-MM-dd strings), excluding Sat/Sun and the given holiday
 * dates, shaving 0.5 off half-day boundaries. The backend remains the source of
 * truth — this only powers the live preview on the submit form.
 *
 * Returns null for incomplete/invalid input (missing dates or end < start).
 */
export function previewWorkingDays(
  start: string,
  end: string,
  startHalf: LeaveHalf,
  endHalf: LeaveHalf,
  holidays: Set<string>,
): number | null {
  if (!start || !end || end < start) return null;

  const isWorking = (iso: string) => {
    // Parse as UTC noon to dodge timezone day-shift.
    const dow = new Date(iso + "T12:00:00Z").getUTCDay(); // 0=Sun, 6=Sat
    if (dow === 0 || dow === 6) return false;
    return !holidays.has(iso);
  };

  const nextDay = (iso: string) => {
    const d = new Date(iso + "T12:00:00Z");
    d.setUTCDate(d.getUTCDate() + 1);
    return d.toISOString().slice(0, 10);
  };

  if (start === end) {
    if (!isWorking(start)) return 0;
    return startHalf !== "FULL_DAY" || endHalf !== "FULL_DAY" ? 0.5 : 1;
  }

  let total = 0;
  for (let d = start; d <= end; d = nextDay(d)) {
    if (isWorking(d)) total += 1;
  }
  if (startHalf !== "FULL_DAY" && isWorking(start)) total -= 0.5;
  if (endHalf !== "FULL_DAY" && isWorking(end)) total -= 0.5;
  return total;
}
