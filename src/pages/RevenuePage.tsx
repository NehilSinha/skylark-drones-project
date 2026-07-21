import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import { KpiCard } from "@/components/KpiCard";
import { LoadingState } from "@/components/LoadingState";
import { ErrorState } from "@/components/ErrorState";
import { WarningsList } from "@/components/WarningsList";

export function RevenuePage() {
  const revenue = useQuery({ queryKey: ["revenue"], queryFn: () => api.revenue() });

  if (revenue.isLoading) return <LoadingState label="Loading revenue…" />;
  if (revenue.error) return <ErrorState error={revenue.error} onRetry={() => revenue.refetch()} />;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold">Revenue</h2>
        <p className="text-sm text-[var(--color-muted)]">Booked, billed, and collected revenue.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard label="Booked Revenue" value={formatCurrency(revenue.data?.bookedRevenue)} hint="Value of Won deals" />
        <KpiCard label="Billed Revenue" value={formatCurrency(revenue.data?.billedRevenue)} />
        <KpiCard label="Collected Revenue" value={formatCurrency(revenue.data?.collectedRevenue)} />
        <KpiCard label="Outstanding Receivable" value={formatCurrency(revenue.data?.outstandingReceivable)} />
      </div>

      <WarningsList warnings={revenue.data?.warnings ?? []} />
    </div>
  );
}
