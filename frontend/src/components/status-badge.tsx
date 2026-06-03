import { cn } from "@/lib/utils";
import { STATUS_LABELS, type LeaveStatus } from "@/features/leave-requests/types";

// Status colours per docs/UI-GUIDELINES.md (text + tinted background, no icon).
// Full literal class strings so Tailwind can statically detect them.
const STATUS_CLASSES: Record<LeaveStatus, string> = {
  PENDING: "bg-amber-100 text-amber-800",
  APPROVED: "bg-green-100 text-green-800",
  REJECTED: "bg-rose-100 text-rose-800",
  CANCELLED: "bg-slate-100 text-slate-700",
};

export function StatusBadge({ status, className }: { status: LeaveStatus; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium",
        STATUS_CLASSES[status],
        className,
      )}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}
