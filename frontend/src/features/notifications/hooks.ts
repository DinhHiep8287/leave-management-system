import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "./api";

const POLL_MS = 30_000;

export function useNotifications(enabled: boolean) {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: () => listNotifications(10),
    enabled,
    refetchInterval: POLL_MS,
    staleTime: 10_000,
  });
}

export function useUnreadCount() {
  return useQuery({
    queryKey: ["notifications-unread"],
    queryFn: getUnreadCount,
    refetchInterval: POLL_MS,
    staleTime: 10_000,
  });
}

function invalidate(qc: ReturnType<typeof useQueryClient>) {
  void qc.invalidateQueries({ queryKey: ["notifications"] });
  void qc.invalidateQueries({ queryKey: ["notifications-unread"] });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: () => invalidate(qc),
  });
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => invalidate(qc),
  });
}
