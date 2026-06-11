import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { TableSkeletonRows } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAuth } from "@/features/auth/auth-context";
import type { LeaveBalance } from "@/features/dashboard/types";

import { AdjustBalanceDialog } from "./adjust-dialog";
import { useCarryOver, useInitializeYear, useUserBalances, useUserOptions } from "./hooks";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = [CURRENT_YEAR + 1, CURRENT_YEAR, CURRENT_YEAR - 1];

export function BalancesPage() {
  const { user: me } = useAuth();
  const isAdmin = me?.role === "ADMIN";

  const [initYear, setInitYear] = useState(CURRENT_YEAR);
  const [userId, setUserId] = useState<number | undefined>(undefined);
  const [year, setYear] = useState(CURRENT_YEAR);
  const [adjustTarget, setAdjustTarget] = useState<LeaveBalance | null>(null);

  const { data: users } = useUserOptions();
  const { data: balances, isFetching } = useUserBalances(userId, year);
  const initialize = useInitializeYear();
  const carryOver = useCarryOver();
  const [carryFromYear, setCarryFromYear] = useState(CURRENT_YEAR - 1);
  const [carryCap, setCarryCap] = useState(5);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Quỹ phép</h1>
        <p className="text-sm text-muted-foreground">Khởi tạo quỹ phép đầu năm và điều chỉnh thủ công.</p>
      </header>

      {isAdmin && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Khởi tạo quỹ phép đầu năm</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap items-end gap-3">
              <div className="space-y-1.5">
                <Label>Năm</Label>
                <Select
                  className="w-32"
                  value={initYear}
                  onChange={(e) => setInitYear(Number(e.target.value))}
                >
                  {YEARS.map((y) => (
                    <option key={y} value={y}>
                      {y}
                    </option>
                  ))}
                </Select>
              </div>
              <Button disabled={initialize.isPending} onClick={() => initialize.mutate(initYear)}>
                {initialize.isPending ? "Đang khởi tạo…" : "Khởi tạo"}
              </Button>
              <p className="text-xs text-muted-foreground">
                Tạo dòng quỹ phép cho mọi nhân viên đang hoạt động (bỏ qua dòng đã có).
              </p>
            </div>
            <div className="mt-4 flex flex-wrap items-end gap-3 border-t border-border pt-4">
              <div className="space-y-1.5">
                <Label>Chuyển phép tồn từ năm</Label>
                <Select
                  className="w-32"
                  value={carryFromYear}
                  onChange={(e) => setCarryFromYear(Number(e.target.value))}
                >
                  {YEARS.map((y) => (
                    <option key={y} value={y}>
                      {y}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Tối đa (ngày)</Label>
                <Select className="w-24" value={carryCap} onChange={(e) => setCarryCap(Number(e.target.value))}>
                  {[3, 5, 7, 10].map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </Select>
              </div>
              <Button
                variant="outline"
                disabled={carryOver.isPending}
                onClick={() => carryOver.mutate({ fromYear: carryFromYear, capDays: carryCap })}
              >
                {carryOver.isPending ? "Đang chuyển…" : "Chuyển phép tồn"}
              </Button>
              <p className="text-xs text-muted-foreground">
                Chuyển phép còn dư sang năm {carryFromYear + 1}; chạy lại không cộng dồn.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Điều chỉnh quỹ phép nhân viên</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-3">
            <div className="space-y-1.5">
              <Label>Nhân viên</Label>
              <Select
                className="w-64"
                value={userId ?? ""}
                onChange={(e) => setUserId(e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">Chọn nhân viên</option>
                {users?.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.fullName} ({u.employeeCode})
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Năm</Label>
              <Select className="w-32" value={year} onChange={(e) => setYear(Number(e.target.value))}>
                {YEARS.map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          {userId == null ? (
            <p className="text-sm text-muted-foreground">Chọn nhân viên để xem quỹ phép.</p>
          ) : (
            <div className="rounded-lg border border-border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Loại</TableHead>
                    <TableHead>Tổng</TableHead>
                    <TableHead>Đã dùng</TableHead>
                    <TableHead>Điều chỉnh</TableHead>
                    <TableHead>Chuyển từ năm trước</TableHead>
                    <TableHead>Còn lại</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isFetching && <TableSkeletonRows rows={4} colSpan={7} />}
                  {!isFetching && (balances?.length ?? 0) === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground">
                        Chưa có quỹ phép cho năm này.
                      </TableCell>
                    </TableRow>
                  )}
                  {balances?.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell className="font-medium">{b.leaveTypeCode}</TableCell>
                      <TableCell>{b.totalDays}</TableCell>
                      <TableCell>{b.usedDays}</TableCell>
                      <TableCell>{b.adjustedDays}</TableCell>
                      <TableCell>{b.carriedOverDays}</TableCell>
                      <TableCell>{b.remainingDays}</TableCell>
                      <TableCell className="text-right">
                        <Button variant="ghost" size="sm" onClick={() => setAdjustTarget(b)}>
                          Điều chỉnh
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      <AdjustBalanceDialog balance={adjustTarget} onClose={() => setAdjustTarget(null)} />
    </div>
  );
}
