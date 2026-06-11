import { api } from "@/lib/api";

import type { Notification } from "./types";

type Envelope<T> = { data: T };
type PageData<T> = { content: T[] };

export async function listNotifications(size = 10): Promise<Notification[]> {
  const res = await api.get<Envelope<PageData<Notification>>>("/notifications", {
    params: { size },
  });
  return res.data.data.content;
}

export async function getUnreadCount(): Promise<number> {
  const res = await api.get<Envelope<number>>("/notifications/unread-count");
  return res.data.data;
}

export async function markNotificationRead(id: number): Promise<Notification> {
  const res = await api.patch<Envelope<Notification>>(`/notifications/${id}/read`);
  return res.data.data;
}

export async function markAllNotificationsRead(): Promise<number> {
  const res = await api.post<Envelope<number>>("/notifications/read-all");
  return res.data.data;
}
