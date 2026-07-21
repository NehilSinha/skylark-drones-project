import type { DataQualityWarning } from "@/lib/types";
import { SeverityBadge } from "@/components/ui/badge";

/** Data-quality caveats must always be visible next to the numbers they qualify — never a silent gap. */
export function WarningsList({ warnings }: { warnings: DataQualityWarning[] }) {
  if (warnings.length === 0) return null;

  return (
    <div className="flex flex-col gap-2 rounded-lg border border-[var(--color-border)] bg-black/5 dark:bg-white/5 p-3">
      {warnings.map((warning) => (
        <div key={warning.code} className="flex items-start gap-2 text-sm">
          <SeverityBadge severity={warning.severity} />
          <span className="text-[var(--color-muted)]">{warning.message}</span>
        </div>
      ))}
    </div>
  );
}
