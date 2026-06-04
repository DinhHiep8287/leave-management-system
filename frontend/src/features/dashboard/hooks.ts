import { useQuery } from "@tanstack/react-query";

import { getAdminSummary, getDashboardSummary } from "./api";

export function useDashboardSummary() {
  return useQuery({ queryKey: ["dashboard"], queryFn: getDashboardSummary });
}

export function useAdminSummary(enabled: boolean) {
  return useQuery({ queryKey: ["dashboard-admin"], queryFn: getAdminSummary, enabled });
}
