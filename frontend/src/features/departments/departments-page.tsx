import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";

import { DepartmentFormDialog } from "./department-form-dialog";
import { useDeleteDepartment, useDepartments } from "./hooks";
import type { Department } from "./types";

export function DepartmentsPage() {
  const [q, setQ] = useState("");
  const [activeOnly, setActiveOnly] = useState(true);
  const [page, setPage] = useState(0);
  const [formDept, setFormDept] = useState<Department | null>(null);
  const [formOpen, setFormOpen] = useState(false);

  const { data, isFetching } = useDepartments(q, activeOnly, page);
  const del = useDeleteDepartment();
  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Phòng ban</h1>
          <p className="text-sm text-muted-foreground">Quản lý phòng ban và trưởng phòng.</p>
        </div>
        <Button
          onClick={() => {
            setFormDept(null);
            setFormOpen(true);
          }}
        >
          Thêm phòng ban
        </Button>
      </header>

      <div className="flex flex-wrap gap-3">
        <Input
          className="w-64"
          placeholder="Tìm theo mã, tên"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setPage(0);
          }}
        />
        <Select
          className="w-40"
          value={activeOnly ? "active" : "all"}
          onChange={(e) => {
            setActiveOnly(e.target.value === "active");
            setPage(0);
          }}
        >
          <option value="active">Đang hoạt động</option>
          <option value="all">Tất cả</option>
        </Select>
        {isFetching && <span className="self-center text-xs text-muted-foreground">Đang tải…</span>}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã</TableHead>
              <TableHead>Tên</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Không có phòng ban.
                </TableCell>
              </TableRow>
            )}
            {items.map((d) => (
              <TableRow key={d.id}>
                <TableCell className="font-medium">{d.code}</TableCell>
                <TableCell>{d.name}</TableCell>
                <TableCell>
                  <span
                    className={cn(
                      "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium",
                      d.active ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-700",
                    )}
                  >
                    {d.active ? "Hoạt động" : "Vô hiệu"}
                  </span>
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setFormDept(d);
                        setFormOpen(true);
                      }}
                    >
                      Sửa
                    </Button>
                    {d.active && (
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={del.isPending}
                        onClick={() => del.mutate(d.id)}
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

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-3 text-sm">
          <span className="text-muted-foreground">
            Trang {page + 1} / {totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
            Trước
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </Button>
        </div>
      )}

      <DepartmentFormDialog open={formOpen} department={formDept} onClose={() => setFormOpen(false)} />
    </div>
  );
}
