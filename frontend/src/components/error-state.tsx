import { Button } from "@/components/ui/button";

/** Inline error notice for a failed data query, with an optional retry. */
export function ErrorState({
  message = "Không tải được dữ liệu. Vui lòng thử lại.",
  onRetry,
}: {
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">
      <p className="text-destructive">{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" className="mt-2" onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  );
}
