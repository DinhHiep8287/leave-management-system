import { formatDate, formatDateTime } from "./format";

describe("formatDate", () => {
  it("returns empty string for null/undefined/empty", () => {
    expect(formatDate(null)).toBe("");
    expect(formatDate(undefined)).toBe("");
    expect(formatDate("")).toBe("");
  });

  it("formats an ISO date as dd/MM/yyyy", () => {
    expect(formatDate("2026-06-11")).toBe("11/06/2026");
  });

  it("formats an ISO date-time by taking the date part", () => {
    expect(formatDate("2026-01-02T08:30:00Z")).toBe("02/01/2026");
  });

  it("returns input unchanged when it does not have three dash-parts", () => {
    expect(formatDate("garbage")).toBe("garbage");
    expect(formatDate("2026-06")).toBe("2026-06");
  });
});

describe("formatDateTime", () => {
  it("returns empty string for null/undefined/empty", () => {
    expect(formatDateTime(null)).toBe("");
    expect(formatDateTime(undefined)).toBe("");
    expect(formatDateTime("")).toBe("");
  });

  it("returns invalid input unchanged", () => {
    expect(formatDateTime("garbage")).toBe("garbage");
  });

  it("formats a valid ISO date-time with the vi-VN locale", () => {
    // vi-VN short style renders "HH:mm d/M/yy". Assert the shape only, so the test
    // stays independent of the runner's timezone (hour/day may shift).
    const out = formatDateTime("2026-06-11T05:00:00+07:00");
    expect(out).toMatch(/^\d{2}:\d{2} \d{1,2}\/\d{1,2}\/26$/);
  });
});
