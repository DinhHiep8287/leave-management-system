import { Link } from "react-router-dom";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
        <p className="text-sm text-muted-foreground">
          Xin chào, <strong className="text-foreground">{user?.fullName}</strong>.
        </p>
      </header>

      <div className="grid gap-4 sm:grid-cols-2">
        <Link to="/leave-requests/new" className="block">
          <Card className="transition-colors hover:border-primary/50">
            <CardHeader>
              <CardTitle className="text-base">Nộp đơn nghỉ phép</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              Tạo đơn mới, hệ thống tự tính số ngày nghỉ.
            </CardContent>
          </Card>
        </Link>
        <Link to="/leave-requests" className="block">
          <Card className="transition-colors hover:border-primary/50">
            <CardHeader>
              <CardTitle className="text-base">Đơn của tôi</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              Theo dõi trạng thái và lịch sử các đơn đã nộp.
            </CardContent>
          </Card>
        </Link>
      </div>

      <p className="text-xs text-muted-foreground">Thống kê và biểu đồ sẽ bổ sung ở bước kế tiếp.</p>
    </div>
  );
}
