import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";

import { useAdminSummary, useDashboardSummary } from "./hooks";

const USED_COLOR = "#0d9488"; // teal-600
const REMAINING_COLOR = "#5eead4"; // teal-300

export function DashboardPage() {
  const { user } = useAuth();
  const isHrAdmin = user?.role === "HR" || user?.role === "ADMIN";
  const { data, isLoading } = useDashboardSummary();
  const { data: admin, isLoading: adminLoading } = useAdminSummary(isHrAdmin);

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

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
        <p className="text-sm text-muted-foreground">
          Xin chào, <strong className="text-foreground">{user?.fullName}</strong>.
        </p>
      </header>

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Chờ bạn duyệt" value={data?.pendingApprovalCount} loading={isLoading} />
        <StatCard label="Đang nghỉ hôm nay" value={data?.onLeaveTodayCount} loading={isLoading} />
        <StatCard label="Đơn chờ của tôi" value={data?.myPendingCount} loading={isLoading} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">Quỹ phép năm nay</CardTitle>
          </CardHeader>
          <CardContent>
            {chartData.length === 0 ? (
              <p className="py-8 text-center text-sm text-muted-foreground">
                Chưa có dữ liệu quỹ phép.
              </p>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
                  <XAxis dataKey="type" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
                  <YAxis tick={{ fontSize: 12 }} tickLine={false} axisLine={false} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid hsl(var(--border))" }}
                  />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="Đã dùng" stackId="a" fill={USED_COLOR} radius={[0, 0, 0, 0]} />
                  <Bar dataKey="Còn lại" stackId="a" fill={REMAINING_COLOR} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Đang nghỉ hôm nay</CardTitle>
          </CardHeader>
          <CardContent>
            {data && data.onLeaveToday.length === 0 ? (
              <p className="py-4 text-sm text-muted-foreground">Không có ai nghỉ hôm nay.</p>
            ) : (
              <ul className="space-y-2 text-sm">
                {data?.onLeaveToday.map((e) => (
                  <li key={e.leaveRequestId} className="flex items-center justify-between">
                    <span>{e.userFullName}</span>
                    <span className="text-xs text-muted-foreground">{e.leaveTypeCode}</span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>

      {isHrAdmin && (
        <section className="space-y-4">
          <h2 className="text-sm font-medium uppercase tracking-wide text-muted-foreground">
            Toàn tổ chức
          </h2>
          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard label="Tổng nhân viên" value={admin?.totalActiveEmployees} loading={adminLoading} />
            <StatCard label="Đơn chờ duyệt" value={admin?.pendingCount} loading={adminLoading} />
            <StatCard label="Đã duyệt" value={admin?.approvedCount} loading={adminLoading} />
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle className="text-base">Đơn theo trạng thái</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={statusData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
                    <XAxis dataKey="status" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
                    <YAxis tick={{ fontSize: 12 }} tickLine={false} axisLine={false} allowDecimals={false} />
                    <Tooltip
                      contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid hsl(var(--border))" }}
                    />
                    <Bar dataKey="Số đơn" fill={USED_COLOR} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Phòng ban nghỉ nhiều (tháng này)</CardTitle>
              </CardHeader>
              <CardContent>
                {admin && admin.topDepartmentsThisMonth.length === 0 ? (
                  <p className="py-4 text-sm text-muted-foreground">Chưa có đơn duyệt tháng này.</p>
                ) : (
                  <ul className="space-y-2 text-sm">
                    {admin?.topDepartmentsThisMonth.map((d) => (
                      <li key={d.departmentId} className="flex items-center justify-between">
                        <span>{d.departmentName}</span>
                        <span className="font-medium">{d.requestCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" size="sm">
              <Link to="/admin/users">Quản lý người dùng</Link>
            </Button>
            <Button asChild variant="outline" size="sm">
              <Link to="/admin/balances">Quỹ phép</Link>
            </Button>
            <Button asChild variant="outline" size="sm">
              <Link to="/admin/holidays">Ngày lễ</Link>
            </Button>
          </div>
        </section>
      )}
    </div>
  );
}

function StatCard({ label, value, loading }: { label: string; value?: number; loading: boolean }) {
  return (
    <Card>
      <CardContent className="pt-6">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="mt-1 text-3xl font-semibold tracking-tight">
          {loading ? "…" : (value ?? 0)}
        </p>
      </CardContent>
    </Card>
  );
}
