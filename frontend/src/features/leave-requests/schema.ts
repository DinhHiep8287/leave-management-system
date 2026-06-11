import { z } from "zod";

import type { LeaveHalf } from "./types";

export const HALVES: LeaveHalf[] = ["FULL_DAY", "MORNING", "AFTERNOON"];

/** Shared form schema for submitting and editing a leave request. */
export const leaveRequestSchema = z
  .object({
    leaveTypeId: z.coerce.number().int().positive("Chọn loại nghỉ phép"),
    startDate: z.string().min(1, "Chọn ngày bắt đầu"),
    endDate: z.string().min(1, "Chọn ngày kết thúc"),
    startHalf: z.enum(["FULL_DAY", "MORNING", "AFTERNOON"]),
    endHalf: z.enum(["FULL_DAY", "MORNING", "AFTERNOON"]),
    reason: z.string().trim().min(1, "Nhập lý do nghỉ").max(2000, "Lý do tối đa 2000 ký tự"),
  })
  .refine((d) => d.endDate >= d.startDate, {
    message: "Ngày kết thúc phải từ ngày bắt đầu trở đi",
    path: ["endDate"],
  });

export type LeaveRequestFormValues = z.infer<typeof leaveRequestSchema>;
