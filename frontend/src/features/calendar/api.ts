import { api } from "@/lib/api";

import type { CalendarEntry, DeptOption, Holiday } from "./types";

type Envelope<T> = { data: T };
type PageData<T> = { content: T[] };

export type CalendarFilters = {
  departmentId?: number;
  leaveTypeId?: number;
  userId?: number;
  includePending: boolean;
};

export async function getCalendar(
  from: string,
  to: string,
  filters: CalendarFilters,
): Promise<CalendarEntry[]> {
  const res = await api.get<Envelope<CalendarEntry[]>>("/calendar", {
    params: {
      from,
      to,
      departmentId: filters.departmentId,
      leaveTypeId: filters.leaveTypeId,
      userId: filters.userId,
      includePending: filters.includePending,
    },
  });
  return res.data.data;
}

export async function getHolidays(year: number): Promise<Holiday[]> {
  const res = await api.get<Envelope<Holiday[]>>("/holidays", { params: { year } });
  return res.data.data;
}

export async function getDepartments(): Promise<DeptOption[]> {
  const res = await api.get<Envelope<PageData<DeptOption>>>("/departments", {
    params: { size: 100, activeOnly: true },
  });
  return res.data.data.content;
}
