import {
  addMonths,
  addWeeks,
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
  subWeeks,
} from "date-fns";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useMemo, useState } from "react";

import { ErrorState } from "@/components/error-state";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { useUserOptions } from "@/features/balances/hooks";
import { useLeaveTypes } from "@/features/leave-requests/hooks";
import { RequestDetailDialog } from "@/features/leave-requests/request-detail-dialog";
import { cn } from "@/lib/utils";

import { useCalendar, useDepartments, useHolidays } from "./hooks";
import type { CalendarEntry } from "./types";

const WEEKDAYS = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];
const WEEK_OPTS = { weekStartsOn: 1 } as const;

type View = "month" | "week";

export function CalendarPage() {
  const { user } = useAuth();
  const isHrAdmin = user?.role === "HR" || user?.role === "ADMIN";

  const [view, setView] = useState<View>("month");
  const [refDate, setRefDate] = useState(() => new Date());
  const [departmentId, setDepartmentId] = useState<number | undefined>(undefined);
  const [leaveTypeId, setLeaveTypeId] = useState<number | undefined>(undefined);
  const [userId, setUserId] = useState<number | undefined>(undefined);
  const [includePending, setIncludePending] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);

  const monthMode = view === "month";

  const grid = useMemo(() => {
    const ws = monthMode ? startOfMonth(refDate) : startOfWeek(refDate, WEEK_OPTS);
    const we = monthMode ? endOfMonth(refDate) : endOfWeek(refDate, WEEK_OPTS);
    const gs = monthMode ? startOfWeek(ws, WEEK_OPTS) : ws;
    const ge = monthMode ? endOfWeek(we, WEEK_OPTS) : we;
    return { winStart: ws, winEnd: we, gs, ge, days: eachDayOfInterval({ start: gs, end: ge }) };
  }, [view, refDate, monthMode]);

  const fromStr = format(grid.winStart, "yyyy-MM-dd");
  const toStr = format(grid.winEnd, "yyyy-MM-dd");

  const { data: types } = useLeaveTypes(true);
  const { data: depts } = useDepartments(isHrAdmin);
  const { data: users } = useUserOptions();
  const { data: entries, isFetching, isError, refetch } = useCalendar(fromStr, toStr, {
    departmentId,
    leaveTypeId,
    userId,
    includePending,
  });
  const { data: holidays } = useHolidays(refDate.getFullYear());

  const holidayByDay = useMemo(() => {
    const map = new Map<string, string>();
    for (const h of holidays ?? []) map.set(h.holidayDate, h.name);
    return map;
  }, [holidays]);

  const entriesByDay = useMemo(() => {
    const map = new Map<string, CalendarEntry[]>();
    for (const e of entries ?? []) {
      const lo = parseISO(e.startDate) < grid.gs ? grid.gs : parseISO(e.startDate);
      const hi = parseISO(e.endDate) > grid.ge ? grid.ge : parseISO(e.endDate);
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

  const navPrev = () => setRefDate((d) => (monthMode ? subMonths(d, 1) : subWeeks(d, 1)));
  const navNext = () => setRefDate((d) => (monthMode ? addMonths(d, 1) : addWeeks(d, 1)));

  const title = monthMode
    ? `Tháng ${format(refDate, "MM/yyyy")}`
    : `Tuần ${format(grid.winStart, "dd/MM")} - ${format(grid.winEnd, "dd/MM/yyyy")}`;

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Lịch nghỉ phép</h1>
          <p className="text-sm text-muted-foreground">Nghỉ phép trong phạm vi của bạn.</p>
        </div>
        <div className="inline-flex overflow-hidden rounded-md border border-border text-sm">
          <button
            type="button"
            onClick={() => setView("month")}
            className={cn("px-3 py-1.5", monthMode ? "bg-primary text-primary-foreground" : "hover:bg-secondary")}
          >
            Tháng
          </button>
          <button
            type="button"
            onClick={() => setView("week")}
            className={cn("px-3 py-1.5", !monthMode ? "bg-primary text-primary-foreground" : "hover:bg-secondary")}
          >
            Tuần
          </button>
        </div>
      </header>

      <div className="flex flex-wrap items-center gap-3">
        <Button variant="outline" size="icon" aria-label="Trước" onClick={navPrev}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <span className="min-w-40 text-center text-sm font-medium">{title}</span>
        <Button variant="outline" size="icon" aria-label="Sau" onClick={navNext}>
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button variant="ghost" size="sm" onClick={() => setRefDate(new Date())}>
          Hôm nay
        </Button>

        <Select
          aria-label="Loại nghỉ"
          className="w-40"
          value={leaveTypeId ?? ""}
          onChange={(e) => setLeaveTypeId(e.target.value ? Number(e.target.value) : undefined)}
        >
          <option value="">Mọi loại nghỉ</option>
          {types?.map((t) => (
            <option key={t.id} value={t.id}>
              {t.code}
            </option>
          ))}
        </Select>

        {isHrAdmin && (
          <>
            <Select
              aria-label="Phòng ban"
              className="w-44"
              value={departmentId ?? ""}
              onChange={(e) => setDepartmentId(e.target.value ? Number(e.target.value) : undefined)}
            >
              <option value="">Mọi phòng ban</option>
              {depts?.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </Select>
            <Select
              aria-label="Nhân viên"
              className="w-52"
              value={userId ?? ""}
              onChange={(e) => setUserId(e.target.value ? Number(e.target.value) : undefined)}
            >
              <option value="">Mọi nhân viên</option>
              {users?.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName}
                </option>
              ))}
            </Select>
          </>
        )}

        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-input"
            checked={includePending}
            onChange={(e) => setIncludePending(e.target.checked)}
          />
          Kèm đơn chờ duyệt
        </label>
        {isFetching && <span className="text-xs text-muted-foreground">Đang tải…</span>}
      </div>

      {isError && <ErrorState onRetry={() => void refetch()} />}

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
            const inWindow = monthMode ? isSameMonth(day, refDate) : true;
            const holiday = holidayByDay.get(key);
            const dimmed = isWeekend(day) || holiday != null;
            const dayEntries = entriesByDay.get(key) ?? [];
            return (
              <div
                key={key}
                className={cn(
                  "border-b border-r border-border p-1.5 last:border-r-0",
                  monthMode ? "min-h-24" : "min-h-48",
                  !inWindow && "bg-muted/30 text-muted-foreground",
                  dimmed && inWindow && "bg-muted/40",
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
                {holiday && inWindow && (
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
                      title={`${e.userFullName} · ${e.leaveTypeCode} — bấm để xem chi tiết`}
                      className={cn(
                        "block w-full cursor-pointer truncate rounded px-1.5 py-0.5 text-left text-[11px]",
                        "transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                        e.status === "APPROVED"
                          ? "bg-primary/15 text-primary hover:bg-primary/30"
                          : "bg-amber-100 text-amber-800 hover:bg-amber-200",
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
