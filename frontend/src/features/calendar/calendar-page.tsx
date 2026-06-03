import {
  addMonths,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  isSameMonth,
  isToday,
  isWeekend,
  parseISO,
  startOfMonth,
  startOfWeek,
  subMonths,
} from "date-fns";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useMemo, useState } from "react";

import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { RequestDetailDialog } from "@/features/leave-requests/request-detail-dialog";
import { cn } from "@/lib/utils";

import { useCalendar, useDepartments, useHolidays } from "./hooks";
import type { CalendarEntry } from "./types";

const WEEKDAYS = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];
const WEEK_OPTS = { weekStartsOn: 1 } as const;

export function CalendarPage() {
  const { user } = useAuth();
  const isHrAdmin = user?.role === "HR" || user?.role === "ADMIN";

  const [monthDate, setMonthDate] = useState(() => startOfMonth(new Date()));
  const [departmentId, setDepartmentId] = useState<number | undefined>(undefined);
  const [detailId, setDetailId] = useState<number | null>(null);

  const fromStr = format(startOfMonth(monthDate), "yyyy-MM-dd");
  const toStr = format(endOfMonth(monthDate), "yyyy-MM-dd");

  const { data: entries, isFetching } = useCalendar(fromStr, toStr, departmentId);
  const { data: holidays } = useHolidays(monthDate.getFullYear());
  const { data: depts } = useDepartments(isHrAdmin);

  const grid = useMemo(() => {
    const start = startOfWeek(startOfMonth(monthDate), WEEK_OPTS);
    const end = endOfWeek(endOfMonth(monthDate), WEEK_OPTS);
    return { start, end, days: eachDayOfInterval({ start, end }) };
  }, [monthDate]);

  const holidayByDay = useMemo(() => {
    const map = new Map<string, string>();
    for (const h of holidays ?? []) map.set(h.holidayDate, h.name);
    return map;
  }, [holidays]);

  const entriesByDay = useMemo(() => {
    const map = new Map<string, CalendarEntry[]>();
    for (const e of entries ?? []) {
      const s = parseISO(e.startDate);
      const en = parseISO(e.endDate);
      const lo = s < grid.start ? grid.start : s;
      const hi = en > grid.end ? grid.end : en;
      if (lo > hi) continue;
      for (const d of eachDayOfInterval({ start: lo, end: hi })) {
        const key = format(d, "yyyy-MM-dd");
        const arr = map.get(key);
        if (arr) arr.push(e);
        else map.set(key, [e]);
      }
    }
    return map;
  }, [entries, grid]);

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Lịch nghỉ phép</h1>
          <p className="text-sm text-muted-foreground">
            Đơn đã duyệt và đang chờ duyệt trong phạm vi của bạn.
          </p>
        </div>
        {isHrAdmin && (
          <Select
            aria-label="Phòng ban"
            className="w-52"
            value={departmentId ?? ""}
            onChange={(e) => setDepartmentId(e.target.value ? Number(e.target.value) : undefined)}
          >
            <option value="">Tất cả phòng ban</option>
            {depts?.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </Select>
        )}
      </header>

      <div className="flex items-center gap-3">
        <Button variant="outline" size="icon" aria-label="Tháng trước" onClick={() => setMonthDate((m) => subMonths(m, 1))}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <span className="min-w-32 text-center text-sm font-medium">
          Tháng {format(monthDate, "MM/yyyy")}
        </span>
        <Button variant="outline" size="icon" aria-label="Tháng sau" onClick={() => setMonthDate((m) => addMonths(m, 1))}>
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button variant="ghost" size="sm" onClick={() => setMonthDate(startOfMonth(new Date()))}>
          Hôm nay
        </Button>
        {isFetching && <span className="text-xs text-muted-foreground">Đang tải…</span>}
      </div>

      <div className="overflow-hidden rounded-lg border border-border">
        <div className="grid grid-cols-7 border-b border-border bg-muted/40 text-xs font-medium text-muted-foreground">
          {WEEKDAYS.map((w) => (
            <div key={w} className="px-2 py-2 text-center">
              {w}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-7">
          {grid.days.map((day) => {
            const key = format(day, "yyyy-MM-dd");
            const inMonth = isSameMonth(day, monthDate);
            const holiday = holidayByDay.get(key);
            const dimmed = isWeekend(day) || holiday != null;
            const dayEntries = entriesByDay.get(key) ?? [];
            return (
              <div
                key={key}
                className={cn(
                  "min-h-24 border-b border-r border-border p-1.5 last:border-r-0",
                  !inMonth && "bg-muted/30 text-muted-foreground",
                  dimmed && inMonth && "bg-muted/40",
                )}
              >
                <div className="flex items-center justify-between">
                  <span
                    className={cn(
                      "text-xs",
                      isToday(day) &&
                        "flex h-5 w-5 items-center justify-center rounded-full bg-primary font-medium text-primary-foreground",
                    )}
                  >
                    {format(day, "d")}
                  </span>
                </div>
                {holiday && inMonth && (
                  <p className="mt-0.5 truncate text-[10px] text-muted-foreground" title={holiday}>
                    {holiday}
                  </p>
                )}
                <div className="mt-1 space-y-1">
                  {dayEntries.map((e) => (
                    <button
                      key={e.leaveRequestId}
                      type="button"
                      onClick={() => setDetailId(e.leaveRequestId)}
                      title={`${e.userFullName} · ${e.leaveTypeCode}`}
                      className={cn(
                        "block w-full truncate rounded px-1.5 py-0.5 text-left text-[11px]",
                        e.status === "APPROVED"
                          ? "bg-primary/15 text-primary"
                          : "bg-amber-100 text-amber-800",
                      )}
                    >
                      {e.userFullName}
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-primary/15" /> Đã duyệt
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-amber-100" /> Chờ duyệt
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-muted" /> Cuối tuần / ngày lễ
        </span>
      </div>

      <RequestDetailDialog requestId={detailId} onClose={() => setDetailId(null)} />
    </div>
  );
}
