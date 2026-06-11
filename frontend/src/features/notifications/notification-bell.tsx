import { Bell } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { RequestDetailDialog } from "@/features/leave-requests/request-detail-dialog";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import { useMarkAllRead, useMarkRead, useNotifications, useUnreadCount } from "./hooks";
import type { Notification } from "./types";

/** Header bell: unread badge, dropdown with the latest notifications, mark-read on click. */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const { data: unread } = useUnreadCount();
  const { data: items } = useNotifications(open);
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
        <div className="absolute right-0 z-50 mt-2 w-80 rounded-lg border border-border bg-popover text-popover-foreground shadow-lg">
          <div className="flex items-center justify-between border-b border-border px-3 py-2">
            <span className="text-sm font-semibold">Thông báo</span>
            {(unread ?? 0) > 0 && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs"
                disabled={markAllRead.isPending}
                onClick={() => markAllRead.mutate()}
              >
                Đánh dấu tất cả đã đọc
              </Button>
            )}
          </div>
          <div className="max-h-96 overflow-y-auto">
            {(items?.length ?? 0) === 0 && (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">
                Chưa có thông báo nào.
              </p>
            )}
            {items?.map((n) => (
              <button
                key={n.id}
                type="button"
                onClick={() => onItemClick(n)}
                className={cn(
                  "block w-full border-b border-border px-3 py-2.5 text-left last:border-b-0 hover:bg-secondary",
                  !n.isRead && "bg-primary/5",
                )}
              >
                <p className={cn("text-sm leading-snug", !n.isRead && "font-medium")}>{n.message}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">{formatDateTime(n.createdAt)}</p>
              </button>
            ))}
          </div>
        </div>
      )}

      <RequestDetailDialog requestId={detailId} onClose={() => setDetailId(null)} />
    </div>
  );
}
