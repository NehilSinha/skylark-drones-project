import type { ReactNode } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function KpiCard({
  label,
  value,
  hint,
  icon,
}: {
  label: string;
  value: string;
  hint?: string;
  icon?: ReactNode;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle>{label}</CardTitle>
        {icon}
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-semibold text-[var(--color-text)]">{value}</div>
        {hint && <p className="mt-1 text-xs text-[var(--color-muted)]">{hint}</p>}
      </CardContent>
    </Card>
  );
}
