import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";

import { useDeleteLeaveType, useLeaveTypesAdmin } from "./hooks";
import { LeaveTypeFormDialog } from "./leave-type-form-dialog";
import type { LeaveType } from "./types";

export function LeaveTypesPage() {
  const [activeOnly, setActiveOnly] = useState(false);
  const [formType, setFormType] = useState<LeaveType | null>(null);
  const [formOpen, setFormOpen] = useState(false);

  const { data: items, isFetching } = useLeaveTypesAdmin(activeOnly);
  const del = useDeleteLeaveType();

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Loại nghỉ phép</h1>
          <p className="text-sm text-muted-foreground">
            Cấu hình loại nghỉ, quota mặc định và việc trừ quỹ phép.
          </p>
        </div>
        <Button
          onClick={() => {
            setFormType(null);
            setFormOpen(true);
          }}
        >
          Thêm loại nghỉ
        </Button>
      </header>

      <div className="flex flex-wrap gap-3">
        <Select
          className="w-40"
          value={activeOnly ? "active" : "all"}
          onChange={(e) => setActiveOnly(e.target.value === "active")}
        >
          <option value="all">Tất cả</option>
          <option value="active">Đang hoạt động</option>
        </Select>
        {isFetching && <span className="self-center text-xs text-muted-foreground">Đang tải…</span>}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã</TableHead>
              <TableHead>Tên</TableHead>
              <TableHead>Quota/năm</TableHead>
              <TableHead>Trừ quỹ phép</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(items?.length ?? 0) === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Không có loại nghỉ phép.
                </TableCell>
              </TableRow>
            )}
            {items?.map((t) => (
              <TableRow key={t.id}>
                <TableCell className="font-medium">{t.code}</TableCell>
                <TableCell>{t.name}</TableCell>
                <TableCell>{t.defaultQuotaDays}</TableCell>
                <TableCell>{t.requiresBalance ? "Có" : "Không"}</TableCell>
                <TableCell>
                  <span
                    className={cn(
                      "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium",
                      t.active ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-700",
                    )}
                  >
                    {t.active ? "Hoạt động" : "Vô hiệu"}
                  </span>
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setFormType(t);
                        setFormOpen(true);
                      }}
                    >
                      Sửa
                    </Button>
                    {t.active && (
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={del.isPending}
                        onClick={() => del.mutate(t.id)}
                      >
                        Vô hiệu hóa
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <LeaveTypeFormDialog open={formOpen} leaveType={formType} onClose={() => setFormOpen(false)} />
    </div>
  );
}
