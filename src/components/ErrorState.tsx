import { AlertTriangle } from "lucide-react";
import { ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message =
    error instanceof ApiError
      ? error.message
      : error instanceof Error
        ? error.message
        : "Something went wrong.";
  const traceId = error instanceof ApiError ? error.traceId : undefined;

  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12 text-center">
      <AlertTriangle className="h-8 w-8 text-[var(--color-danger)]" />
      <p className="text-sm text-[var(--color-text)]">{message}</p>
      {traceId && <p className="text-xs text-[var(--color-muted)]">Reference: {traceId}</p>}
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}
