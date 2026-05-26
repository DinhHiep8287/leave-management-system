import { useQuery } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";

type HealthData = { status: string; app: string };
type ApiEnvelope<T> = { data: T; error: unknown; meta: unknown };

async function fetchHealth(): Promise<HealthData> {
  const res = await api.get<ApiEnvelope<HealthData>>("/health");
  return res.data.data;
}

export default function App() {
  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ["health"],
    queryFn: fetchHealth,
  });

  const statusLabel = isLoading
    ? "Đang kiểm tra…"
    : isError
      ? "Không kết nối được"
      : (data?.status ?? "—");

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="w-full max-w-md space-y-6 rounded-lg border border-border bg-card p-8 shadow-sm">
        <header>
          <h1 className="text-2xl font-semibold tracking-tight">
            Leave Management
          </h1>
          <p className="text-sm text-muted-foreground">
            Skeleton — Tuần 1 Foundation
          </p>
        </header>

        <section className="space-y-2 rounded-md border border-border bg-muted/40 p-4">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">API</span>
            <span
              className={
                isError
                  ? "font-medium text-destructive"
                  : "font-medium text-foreground"
              }
            >
              {statusLabel}
            </span>
          </div>
          {data?.app ? (
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">App</span>
              <span className="font-mono text-xs">{data.app}</span>
            </div>
          ) : null}
        </section>

        <Button
          onClick={() => refetch()}
          disabled={isFetching}
          className="w-full"
        >
          {isFetching ? "Đang làm mới…" : "Kiểm tra lại"}
        </Button>
      </div>
    </div>
  );
}
