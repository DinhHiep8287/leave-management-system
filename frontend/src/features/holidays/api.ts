import { api } from "@/lib/api";
import type { Holiday } from "@/features/calendar/types";

type Envelope<T> = { data: T };

export type HolidayRequest = {
  holidayDate: string;
  name: string;
  description: string | null;
};

export async function createHoliday(body: HolidayRequest): Promise<Holiday> {
  const res = await api.post<Envelope<Holiday>>("/holidays", body);
  return res.data.data;
}

export async function updateHoliday(id: number, body: HolidayRequest): Promise<Holiday> {
  const res = await api.put<Envelope<Holiday>>(`/holidays/${id}`, body);
  return res.data.data;
}

export async function deleteHoliday(id: number): Promise<void> {
  await api.delete(`/holidays/${id}`);
}
