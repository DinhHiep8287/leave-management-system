import { previewWorkingDays } from "./working-days";

// Mirrors backend LeaveDayCalculatorTest cases: 2026-07-06 is a Monday.
const NO_HOLIDAYS = new Set<string>();

describe("previewWorkingDays", () => {
  it("counts a full Mon-Fri week as 5 days", () => {
    expect(previewWorkingDays("2026-07-06", "2026-07-10", "FULL_DAY", "FULL_DAY", NO_HOLIDAYS)).toBe(5);
  });

  it("skips weekends when the range spans them", () => {
    // Fri .. Tue → Fri, Mon, Tue = 3
    expect(previewWorkingDays("2026-07-10", "2026-07-14", "FULL_DAY", "FULL_DAY", NO_HOLIDAYS)).toBe(3);
  });

  it("excludes holidays inside the range", () => {
    const holidays = new Set(["2026-07-08"]); // Wednesday
    expect(previewWorkingDays("2026-07-06", "2026-07-10", "FULL_DAY", "FULL_DAY", holidays)).toBe(4);
  });

  it("handles half days at both boundaries", () => {
    expect(previewWorkingDays("2026-07-06", "2026-07-10", "AFTERNOON", "MORNING", NO_HOLIDAYS)).toBe(4);
  });

  it("single working day full vs half", () => {
    expect(previewWorkingDays("2026-07-06", "2026-07-06", "FULL_DAY", "FULL_DAY", NO_HOLIDAYS)).toBe(1);
    expect(previewWorkingDays("2026-07-06", "2026-07-06", "MORNING", "MORNING", NO_HOLIDAYS)).toBe(0.5);
  });

  it("returns 0 for an all-weekend range", () => {
    expect(previewWorkingDays("2026-07-11", "2026-07-12", "FULL_DAY", "FULL_DAY", NO_HOLIDAYS)).toBe(0);
  });

  it("returns null for incomplete or inverted input", () => {
    expect(previewWorkingDays("", "2026-07-10", "FULL_DAY", "FULL_DAY", NO_HOLIDAYS)).toBeNull();
    expect(previewWorkingDays("2026-07-10", "2026-07-06", "FULL_DAY", "FULL_DAY", NO_HOLIDAYS)).toBeNull();
  });

  it("does not shave half off a weekend boundary", () => {
    // Sat..Mon with AFTERNOON start: Sat not working → only Mon counts, no shave for start
    expect(previewWorkingDays("2026-07-11", "2026-07-13", "AFTERNOON", "FULL_DAY", NO_HOLIDAYS)).toBe(1);
  });
});
