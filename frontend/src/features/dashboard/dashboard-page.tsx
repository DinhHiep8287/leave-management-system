import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { ErrorState } from "@/components/error-state";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils";

import { useAdminSummary, useDashboardSummary } from "./hooks";
import type { DepartmentLeaveCount } from "./types";

const USED_COLOR = "#0d9488";
const REMAINING_COLOR = "#5eead4";

const ROLE_LABELS = {
  EMPLOYEE: "Nhân viên",
  MANAGER: "Quản lý",
  HR: "Nhân sự",
  ADMIN: "Quản trị",
} as const;

export function DashboardPage() {
  const { user } = useAuth();
  const isHrAdmin = user?.role === "HR" || user?.role === "ADMIN";
  const { data, isLoading, isError, refetch } = useDashboardSummary();
  const { data: admin, isLoading: adminLoading } = useAdminSummary(isHrAdmin);

  const todayLabel = useMemo(
    () => new Intl.DateTimeFormat("vi-VN", { weekday: "long", day: "2-digit", month: "2-digit" }).format(new Date()),
    [],
  );

  const statusData = useMemo(
    () =>
      admin
        ? [
            { status: "Chờ duyệt", "Số đơn": admin.pendingCount },
            { status: "Đã duyệt", "Số đơn": admin.approvedCount },
            { status: "Từ chối", "Số đơn": admin.rejectedCount },
            { status: "Đã hủy", "Số đơn": admin.cancelledCount },
          ]
        : [],
    [admin],
  );

  const chartData = useMemo(
    () =>
      (data?.myBalances ?? []).map((b) => ({
        type: b.leaveTypeCode,
        "Đã dùng": b.usedDays,
        "Còn lại": b.remainingDays,
      })),
    [data],
  );

  const balanceTotals = useMemo(() => {
    const balances = data?.myBalances ?? [];
    return {
      total: balances.reduce((sum, b) => sum + Number(b.totalDays) + Number(b.adjustedDays), 0),
      used: balances.reduce((sum, b) => sum + Number(b.usedDays), 0),
      remaining: balances.reduce((sum, b) => sum + Number(b.remainingDays), 0),
    };
  }, [data]);

  const quickActions = [
    { label: "Nộp đơn nghỉ", to: "/leave-requests/new", variant: "default" as const },
    { label: "Xem đơn của tôi", to: "/leave-requests", variant: "outline" as const },
    { label: "Mở lịch nghỉ", to: "/calendar", variant: "outline" as const },
    ...(data?.pendingApprovalCount
      ? [{ label: "Duyệt đơn chờ", to: "/approvals", variant: "outline" as const }]
      : []),
    ...(isHrAdmin ? [{ label: "Báo cáo", to: "/reports", variant: "outline" as const }] : []),
  ];

  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <Badge variant="neutral">{user?.role ? ROLE_LABELS[user.role] : "Người dùng"}</Badge>
              <span className="text-xs text-muted-foreground">{todayLabel}</span>
            </div>
            <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
              Xin chào, <strong className="text-foreground">{user?.fullName}</strong>. Đây là các việc cần
              chú ý và số liệu nghỉ phép đang liên quan đến bạn.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {quickActions.map((action) => (
              <Button key={action.to} asChild variant={action.variant} size="sm">
                <Link to={action.to}>{action.label}</Link>
              </Button>
            ))}
          </div>
        </div>
      </section>

      {isError && <ErrorState onRetry={() => void refetch()} />}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Chờ bạn duyệt"
          value={data?.pendingApprovalCount}
          helper={data?.pendingApprovalCount ? "Cần xử lý để nhân viên nhận phản hồi sớm." : "Không có đơn cần xử lý."}
          loading={isLoading}
          tone={data?.pendingApprovalCount ? "attention" : "neutral"}
        />
        <StatCard
          label="Đang nghỉ hôm nay"
          value={data?.onLeaveTodayCount}
          helper="Số người nghỉ trong phạm vi bạn được xem."
          loading={isLoading}
        />
        <StatCard
          label="Đang nghỉ tuần này"
          value={data?.onLeaveThisWeekCount}
          helper="Đếm người nghỉ đã duyệt trong tuần hiện tại."
          loading={isLoading}
        />
        <StatCard
          label="Đơn chờ của tôi"
          value={data?.myPendingCount}
          helper={data?.myPendingCount ? "Theo dõi để biết trạng thái phê duyệt." : "Bạn không có đơn đang chờ."}
          loading={isLoading}
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <CardTitle className="text-base">Quỹ phép năm nay</CardTitle>
                <CardDescription>So sánh số ngày đã dùng và còn lại theo từng loại nghỉ.</CardDescription>
              </div>
              <div className="text-sm text-muted-foreground">
                Còn lại <span className="font-semibold text-foreground">{formatNumber(balanceTotals.remaining)}</span> ngày
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            {chartData.length === 0 ? (
              <p className="py-8 text-center text-sm text-muted-foreground">Chưa có dữ liệu quỹ phép.</p>
            ) : (
              <>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
                    <XAxis dataKey="type" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
                    <YAxis tick={{ fontSize: 12 }} tickLine={false} axisLine={false} allowDecimals={false} />
                    <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid hsl(var(--border))" }} />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                    <Bar dataKey="Đã dùng" stackId="a" fill={USED_COLOR} radius={[0, 0, 0, 0]} />
                    <Bar dataKey="Còn lại" stackId="a" fill={REMAINING_COLOR} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
                <div className="grid gap-3 md:grid-cols-2">
                  {data?.myBalances.map((balance) => (
                    <BalanceProgress key={balance.id} balance={balance} />
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Đang nghỉ hôm nay</CardTitle>
            <CardDescription>Danh sách người nghỉ đã duyệt trong phạm vi của bạn.</CardDescription>
          </CardHeader>
          <CardContent>
            {data && data.onLeaveToday.length === 0 ? (
              <p className="py-6 text-sm text-muted-foreground">Không có ai nghỉ hôm nay.</p>
            ) : (
              <ul className="space-y-3 text-sm">
                {data?.onLeaveToday.map((e) => (
                  <li key={e.leaveRequestId} className="rounded-md border border-border px-3 py-2">
                    <div className="flex items-center justify-between gap-3">
                      <span className="font-medium">{e.userFullName}</span>
                      <span className="text-xs text-muted-foreground">{e.leaveTypeCode}</span>
                    </div>
                    {e.departmentName && (
                      <p className="mt-1 text-xs text-muted-foreground">{e.departmentName}</p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>

      {isHrAdmin && (
        <section className="space-y-4">
          <div>
            <h2 className="text-sm font-medium uppercase tracking-wide text-muted-foreground">Toàn tổ chức</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Số liệu phục vụ HR/Admin theo dõi tải vận hành và trạng thái đơn nghỉ.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            <StatCard label="Nhân viên hoạt động" value={admin?.totalActiveEmployees} loading={adminLoading} />
            <StatCard label="Chờ duyệt" value={admin?.pendingCount} loading={adminLoading} tone="attention" />
            <StatCard label="Đã duyệt" value={admin?.approvedCount} loading={adminLoading} />
            <StatCard label="Từ chối" value={admin?.rejectedCount} loading={adminLoading} />
            <StatCard label="Đã hủy" value={admin?.cancelledCount} loading={adminLoading} />
          </div>

          <div className="grid gap-4 xl:grid-cols-3">
            <Card className="xl:col-span-2">
              <CardHeader>
                <CardTitle className="text-base">Đơn theo trạng thái</CardTitle>
                <CardDescription>Tổng số đơn trong hệ thống, chia theo trạng thái hiện tại.</CardDescription>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={statusData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
                    <XAxis dataKey="status" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
                    <YAxis tick={{ fontSize: 12 }} tickLine={false} axisLine={false} allowDecimals={false} />
                    <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid hsl(var(--border))" }} />
                    <Bar dataKey="Số đơn" fill={USED_COLOR} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            <TopDepartmentsCard departments={admin?.topDepartmentsThisMonth ?? []} loading={adminLoading} />
          </div>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Lối tắt quản trị</CardTitle>
              <CardDescription>Các màn hình thường dùng khi kiểm tra dữ liệu hoặc xử lý cuối kỳ.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-2">
              <Button asChild variant="outline" size="sm">
                <Link to="/admin/users">Quản lý người dùng</Link>
              </Button>
              <Button asChild variant="outline" size="sm">
                <Link to="/admin/balances">Quỹ phép</Link>
              </Button>
              <Button asChild variant="outline" size="sm">
                <Link to="/admin/holidays">Ngày lễ</Link>
              </Button>
              <Button asChild variant="outline" size="sm">
                <Link to="/reports">Báo cáo</Link>
              </Button>
            </CardContent>
          </Card>
        </section>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  loading,
  helper,
  tone = "neutral",
}: {
  label: string;
  value?: number;
  loading: boolean;
  helper?: string;
  tone?: "neutral" | "attention";
}) {
  return (
    <Card className={cn(tone === "attention" && "border-amber-200 bg-amber-50/60 dark:bg-amber-950/20")}>
      <CardContent className="p-5">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="mt-2 text-3xl font-semibold tracking-tight">{loading ? "..." : (value ?? 0)}</p>
        {helper && <p className="mt-2 text-xs leading-relaxed text-muted-foreground">{helper}</p>}
      </CardContent>
    </Card>
  );
}

function BalanceProgress({
  balance,
}: {
  balance: {
    leaveTypeCode: string;
    totalDays: number;
    adjustedDays: number;
    usedDays: number;
    remainingDays: number;
  };
}) {
  const total = Math.max(Number(balance.totalDays) + Number(balance.adjustedDays), 0);
  const used = Math.max(Number(balance.usedDays), 0);
  const percent = total > 0 ? Math.min(100, Math.round((used / total) * 100)) : 0;

  return (
    <div className="rounded-md border border-border p-3">
      <div className="flex items-center justify-between gap-3 text-sm">
        <span className="font-medium">{balance.leaveTypeCode}</span>
        <span className="text-xs text-muted-foreground">
          {formatNumber(used)} / {formatNumber(total)} ngày
        </span>
      </div>
      <div className="mt-2 h-2 rounded-full bg-secondary">
        <div className="h-2 rounded-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
      <p className="mt-2 text-xs text-muted-foreground">Còn lại {formatNumber(balance.remainingDays)} ngày</p>
    </div>
  );
}

function TopDepartmentsCard({
  departments,
  loading,
}: {
  departments: DepartmentLeaveCount[];
  loading: boolean;
}) {
  const max = Math.max(...departments.map((d) => d.requestCount), 1);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Phòng ban nghỉ nhiều</CardTitle>
        <CardDescription>Top phòng ban có đơn đã duyệt trong tháng này.</CardDescription>
      </CardHeader>
      <CardContent>
        {loading ? (
          <p className="py-6 text-sm text-muted-foreground">Đang tải số liệu...</p>
        ) : departments.length === 0 ? (
          <p className="py-6 text-sm text-muted-foreground">Chưa có đơn duyệt tháng này.</p>
        ) : (
          <ul className="space-y-3 text-sm">
            {departments.map((d) => (
              <li key={d.departmentId}>
                <div className="flex items-center justify-between gap-3">
                  <span className="truncate font-medium">{d.departmentName ?? "Chưa rõ phòng ban"}</span>
                  <span className="text-xs text-muted-foreground">{d.requestCount} đơn</span>
                </div>
                <div className="mt-1.5 h-2 rounded-full bg-secondary">
                  <div
                    className="h-2 rounded-full bg-primary"
                    style={{ width: `${Math.max(8, (d.requestCount / max) * 100)}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(value);
}
