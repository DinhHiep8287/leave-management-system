import { leaveRequestSchema } from "./schema";

const valid = {
  leaveTypeId: "1",
  startDate: "2026-07-06",
  endDate: "2026-07-08",
  startHalf: "FULL_DAY",
  endHalf: "FULL_DAY",
  reason: "Về quê",
};

describe("leaveRequestSchema", () => {
  it("accepts a valid request and coerces leaveTypeId to a number", () => {
    const out = leaveRequestSchema.parse(valid);
    expect(out.leaveTypeId).toBe(1);
    expect(out.reason).toBe("Về quê");
  });

  it("rejects endDate before startDate with the message on endDate", () => {
    const r = leaveRequestSchema.safeParse({ ...valid, endDate: "2026-07-01" });
    expect(r.success).toBe(false);
    if (!r.success) {
      const issue = r.error.issues.find((i) => i.path.join(".") === "endDate");
      expect(issue?.message).toBe("Ngày kết thúc phải từ ngày bắt đầu trở đi");
    }
  });

  it("accepts a same-day range", () => {
    expect(leaveRequestSchema.safeParse({ ...valid, endDate: valid.startDate }).success).toBe(true);
  });

  it("rejects an empty or whitespace-only reason", () => {
    expect(leaveRequestSchema.safeParse({ ...valid, reason: "" }).success).toBe(false);
    expect(leaveRequestSchema.safeParse({ ...valid, reason: "   " }).success).toBe(false);
  });

  it("rejects a reason longer than 2000 characters", () => {
    expect(leaveRequestSchema.safeParse({ ...valid, reason: "x".repeat(2001) }).success).toBe(false);
  });

  it("rejects a non-positive leaveTypeId", () => {
    expect(leaveRequestSchema.safeParse({ ...valid, leaveTypeId: "0" }).success).toBe(false);
    expect(leaveRequestSchema.safeParse({ ...valid, leaveTypeId: "-2" }).success).toBe(false);
  });

  it("rejects an unknown half-day value", () => {
    expect(leaveRequestSchema.safeParse({ ...valid, startHalf: "EVENING" }).success).toBe(false);
  });
});
