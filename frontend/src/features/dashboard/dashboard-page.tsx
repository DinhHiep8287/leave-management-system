import { useQuery } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { api } from "@/lib/api";

type HealthData = { status: string; app: string };
type ApiEnvelope<T> = { data: T };

async function fetchHealth(): Promise<HealthData> {
  const res = await api.get<ApiEnvelope<HealthData>>("/health");
  return res.data.data;
}

export function DashboardPage() {
  const { user, logout } = useAuth();
  const { data, isLoading } = useQuery({ queryKey: ["health"], queryFn: fetchHealth });

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="mx-auto max-w-2xl space-y-6">
        <header className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
            <p className="text-sm text-muted-foreground">
              Xin chào, <strong>{user?.fullName}</strong> ({user?.role})
            </p>
          </div>
          <Button variant="outline" onClick={() => void logout()}>
            Đăng xuất
          </Button>
        </header>

        <section className="rounded-md border border-border bg-card p-6 text-sm">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">API status</span>
            <span className="font-medium">{isLoading ? "…" : (data?.status ?? "—")}</span>
          </div>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-muted-foreground">Email</span>
            <span className="font-mono text-xs">{user?.email}</span>
          </div>
        </section>

        <p className="text-xs text-muted-foreground">
          Tuần 2 — Auth foundation. CRUD UI sẽ thêm ở các tuần sau.
        </p>
      </div>
    </div>
  );
}
