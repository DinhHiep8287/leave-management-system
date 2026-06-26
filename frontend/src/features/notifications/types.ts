export type NotificationEventType = "CREATED" | "UPDATED" | "APPROVED" | "REJECTED" | "CANCELLED";

export type Notification = {
  id: number;
  leaveRequestId: number | null;
  eventType: NotificationEventType;
  message: string;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
};
