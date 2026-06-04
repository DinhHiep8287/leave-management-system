import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useHolidays } from "@/features/calendar/hooks";
import type { Holiday } from "@/features/calendar/types";
import { formatDate } from "@/lib/format";

import { HolidayFormDialog } from "./holiday-form-dialog";
import { useDeleteHoliday } from "./hooks";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = [CURRENT_YEAR + 1, CURRENT_YEAR, CURRENT_YEAR - 1];

export function HolidaysPage() {
  const [year, setYear] = useState(CURRENT_YEAR);
  const [formHoliday, setFormHoliday] = useState<Holiday | null>(null);
  const [formOpen, setFormOpen] = useState(false);

  const { data: holidays, isFetching } = useHolidays(year);
  const del = useDeleteHoliday();

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Ngày lễ</h1>
          <p className="text-sm text-muted-foreground">
            Danh sách ngày lễ áp dụng khi tính số ngày nghỉ.
          </p>
        </div>
        <Button
          onClick={() => {
            setFormHoliday(null);
            setFormOpen(true);
          }}
        >
          Thêm ngày lễ
        </Button>
      </header>

      <div className="flex items-center gap-3">
        <Select className="w-40" value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {YEARS.map((y) => (
            <option key={y} value={y}>
              Năm {y}
            </option>
          ))}
        </Select>
        {isFetching && <span className="text-xs text-muted-foreground">Đang tải…</span>}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Ngày</TableHead>
              <TableHead>Tên</TableHead>
              <TableHead>Mô tả</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(holidays?.length ?? 0) === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Chưa có ngày lễ cho năm này.
                </TableCell>
              </TableRow>
            )}
            {holidays?.map((h) => (
              <TableRow key={h.id}>
                <TableCell className="font-medium">{formatDate(h.holidayDate)}</TableCell>
                <TableCell>{h.name}</TableCell>
                <TableCell className="text-muted-foreground">{h.description}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setFormHoliday(h);
                        setFormOpen(true);
                      }}
                    >
                      Sửa
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={del.isPending}
                      onClick={() => del.mutate(h.id)}
                    >
                      Xóa
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <HolidayFormDialog open={formOpen} holiday={formHoliday} onClose={() => setFormOpen(false)} />
    </div>
  );
}
