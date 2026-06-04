import { useMemo, useState } from "react";

import { ErrorState } from "@/components/error-state";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils";

import { useDepartmentOptions, useSetUserActive, useUsers } from "./hooks";
import { ResetPasswordDialog } from "./reset-password-dialog";
import { UserFormDialog } from "./user-form-dialog";
import { ROLES, ROLE_LABELS, type Role, type User } from "./types";

export function UsersPage() {
  const { user: me } = useAuth();
  const isAdmin = me?.role === "ADMIN";

  const [q, setQ] = useState("");
  const [role, setRole] = useState<Role | undefined>(undefined);
  const [departmentId, setDepartmentId] = useState<number | undefined>(undefined);
  const [activeOnly, setActiveOnly] = useState(true);
  const [page, setPage] = useState(0);

  const [formUser, setFormUser] = useState<User | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [resetUser, setResetUser] = useState<User | null>(null);

  const { data, isFetching, isError, refetch } = useUsers({ q, role, departmentId, activeOnly, page });
  const { data: depts } = useDepartmentOptions();
  const setActive = useSetUserActive();

  const deptName = useMemo(() => {
    const m = new Map<number, string>();
    for (const d of depts ?? []) m.set(d.id, d.name);
    return m;
  }, [depts]);

  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 1;

  const resetPage = () => setPage(0);

  const openCreate = () => {
    setFormUser(null);
    setFormOpen(true);
  };
  const openEdit = (u: User) => {
    setFormUser(u);
    setFormOpen(true);
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Người dùng</h1>
          <p className="text-sm text-muted-foreground">Quản lý tài khoản, vai trò và phòng ban.</p>
        </div>
        {isAdmin && <Button onClick={openCreate}>Thêm người dùng</Button>}
      </header>

      {isError && <ErrorState onRetry={() => void refetch()} />}

      <div className="flex flex-wrap gap-3">
        <Input
          className="w-64"
          placeholder="Tìm theo tên, email, mã NV"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            resetPage();
          }}
        />
        <Select
          className="w-40"
          value={role ?? ""}
          onChange={(e) => {
            setRole((e.target.value || undefined) as Role | undefined);
            resetPage();
          }}
        >
          <option value="">Mọi vai trò</option>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {ROLE_LABELS[r]}
            </option>
          ))}
        </Select>
        <Select
          className="w-48"
          value={departmentId ?? ""}
          onChange={(e) => {
            setDepartmentId(e.target.value ? Number(e.target.value) : undefined);
            resetPage();
          }}
        >
          <option value="">Mọi phòng ban</option>
          {depts?.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </Select>
        <Select
          className="w-40"
          value={activeOnly ? "active" : "all"}
          onChange={(e) => {
            setActiveOnly(e.target.value === "active");
            resetPage();
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
              <TableHead>Mã NV</TableHead>
              <TableHead>Họ tên</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Vai trò</TableHead>
              <TableHead>Phòng ban</TableHead>
              <TableHead>Trạng thái</TableHead>
              {isAdmin && <TableHead className="text-right">Thao tác</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.length === 0 && (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 6} className="text-center text-muted-foreground">
                  Không có người dùng.
                </TableCell>
              </TableRow>
            )}
            {items.map((u) => (
              <TableRow key={u.id}>
                <TableCell className="font-medium">{u.employeeCode}</TableCell>
                <TableCell>{u.fullName}</TableCell>
                <TableCell className="text-muted-foreground">{u.email}</TableCell>
                <TableCell>{ROLE_LABELS[u.role]}</TableCell>
                <TableCell>{u.departmentId != null ? deptName.get(u.departmentId) : ""}</TableCell>
                <TableCell>
                  <span
                    className={cn(
                      "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium",
                      u.active ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-700",
                    )}
                  >
                    {u.active ? "Hoạt động" : "Đã khóa"}
                  </span>
                </TableCell>
                {isAdmin && (
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(u)}>
                        Sửa
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setResetUser(u)}>
                        Đặt lại MK
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={setActive.isPending}
                        onClick={() => setActive.mutate({ id: u.id, active: !u.active })}
                      >
                        {u.active ? "Khóa" : "Kích hoạt"}
                      </Button>
                    </div>
                  </TableCell>
                )}
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

      <UserFormDialog open={formOpen} user={formUser} onClose={() => setFormOpen(false)} />
      <ResetPasswordDialog user={resetUser} onClose={() => setResetUser(null)} />
    </div>
  );
}
