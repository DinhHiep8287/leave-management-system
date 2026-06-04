import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { getCalendar, getDepartments, getHolidays, type CalendarFilters } from "./api";

export function useCalendar(from: string, to: string, filters: CalendarFilters) {
  return useQuery({
    queryKey: ["calendar", from, to, filters],
    queryFn: () => getCalendar(from, to, filters),
    placeholderData: keepPreviousData,
  });
}

export function useHolidays(year: number) {
  return useQuery({
    queryKey: ["holidays", year],
    queryFn: () => getHolidays(year),
    staleTime: 60 * 60_000,
  });
}

export function useDepartments(enabled: boolean) {
  return useQuery({
    queryKey: ["departments-all"],
    queryFn: getDepartments,
    enabled,
    staleTime: 60 * 60_000,
  });
}
