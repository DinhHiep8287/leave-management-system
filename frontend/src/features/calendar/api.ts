import { api } from "@/lib/api";

import type { CalendarEntry, DeptOption, Holiday } from "./types";

type Envelope<T> = { data: T };
type PageData<T> = { content: T[] };

export async function getCalendar(
  from: string,
  to: string,
  departmentId?: number,
): Promise<CalendarEntry[]> {
  const res = await api.get<Envelope<CalendarEntry[]>>("/calendar", {
    params: { from, to, departmentId },
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
