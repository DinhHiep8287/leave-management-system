import { Bell } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { RequestDetailDialog } from "@/features/leave-requests/request-detail-dialog";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import { useMarkAllRead, useMarkRead, useNotifications, useUnreadCount } from "./hooks";
import type { Notification, NotificationEventType } from "./types";

const EVENT_LABELS: Record<NotificationEventType, string> = {
  CREATED: "Đơn mới",
  UPDATED: "Cập nhật",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  CANCELLED: "Đã hủy",
};

/** Header bell: unread badge, filterable dropdown, mark-read, and request detail navigation. */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const { data: unread } = useUnreadCount();
  const {
    data: items,
    isError,
    isLoading,
  } = useNotifications(open, unreadOnly);
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();

  useEffect(() => {
    if (!open) return;
    const onClickOutside = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, [open]);

  const onItemClick = (n: Notification) => {
    if (!n.isRead) markRead.mutate(n.id);
    if (n.leaveRequestId != null) {
      setDetailId(n.leaveRequestId);
      setOpen(false);
    }
  };

  return (
    <div className="relative" ref={panelRef}>
      <button
        type="button"
        aria-label={`Thông báo${unread ? ` (${unread} chưa đọc)` : ""}`}
        className="relative rounded-md border border-border p-2 hover:bg-secondary"
        onClick={() => setOpen((o) => !o)}
      >
        <Bell className="h-4 w-4" aria-hidden="true" />
        {(unread ?? 0) > 0 && (
          <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground">
            {unread! > 9 ? "9+" : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-96 max-w-[calc(100vw-2rem)] rounded-lg border border-border bg-popover text-popover-foreground shadow-lg">
          <div className="border-b border-border px-3 py-2">
            <div className="flex items-center justify-between gap-2">
              <div>
                <p className="text-sm font-semibold">Thông báo</p>
                <p className="text-xs text-muted-foreground">
                  {unread ? `${unread} thông báo chưa đọc` : "Không có thông báo mới"}
                </p>
              </div>
              {(unread ?? 0) > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs"
                  disabled={markAllRead.isPending}
                  onClick={() => markAllRead.mutate()}
                >
                  Đánh dấu đã đọc
                </Button>
              )}
            </div>

            <div className="mt-2 flex gap-1 rounded-md bg-secondary p-1">
              <FilterButton active={!unreadOnly} onClick={() => setUnreadOnly(false)}>
                Tất cả
              </FilterButton>
              <FilterButton active={unreadOnly} onClick={() => setUnreadOnly(true)}>
                Chưa đọc
              </FilterButton>
            </div>
          </div>

          <div className="max-h-96 overflow-y-auto">
            {isLoading && (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">
                Đang tải thông báo...
              </p>
            )}
            {isError && (
              <p className="px-3 py-6 text-center text-sm text-destructive">
                Không tải được thông báo. Vui lòng thử lại.
              </p>
            )}
            {!isLoading && !isError && (items?.length ?? 0) === 0 && (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">
                {unreadOnly ? "Không còn thông báo chưa đọc." : "Chưa có thông báo nào."}
              </p>
            )}
            {items?.map((n) => (
              <NotificationItem key={n.id} notification={n} onClick={() => onItemClick(n)} />
            ))}
          </div>
        </div>
      )}

      <RequestDetailDialog requestId={detailId} onClose={() => setDetailId(null)} />
    </div>
  );
}

function FilterButton({
  active,
  children,
  onClick,
}: {
  active: boolean;
  children: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={cn(
        "flex-1 rounded px-2 py-1 text-xs font-medium transition-colors",
        active ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground",
      )}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function NotificationItem({
  notification,
  onClick,
}: {
  notification: Notification;
  onClick: () => void;
}) {
  const readLabel = notification.isRead ? "Đã đọc" : "Mới";

  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "block w-full border-b border-border px-3 py-3 text-left last:border-b-0 hover:bg-secondary",
        !notification.isRead && "bg-primary/5",
      )}
    >
      <div className="mb-1.5 flex items-start justify-between gap-2">
        <Badge variant="neutral">{EVENT_LABELS[notification.eventType]}</Badge>
        <Badge variant={notification.isRead ? "outline" : "default"}>{readLabel}</Badge>
      </div>
      <p className={cn("text-sm leading-snug", !notification.isRead && "font-medium")}>
        {notification.message}
      </p>
      <p className="mt-1 text-xs text-muted-foreground">
        {formatDateTime(notification.createdAt)}
        {notification.readAt ? ` · đọc lúc ${formatDateTime(notification.readAt)}` : ""}
      </p>
    </button>
  );
}
