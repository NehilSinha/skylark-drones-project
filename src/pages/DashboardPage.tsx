import { useQuery } from "@tanstack/react-query";
import { AlertCircle, Banknote, Clock, TrendingUp } from "lucide-react";
import { api } from "@/lib/api";
import { formatCurrency, formatPercent } from "@/lib/utils";
import { KpiCard } from "@/components/KpiCard";
import { LoadingState } from "@/components/LoadingState";
import { ErrorState } from "@/components/ErrorState";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export function DashboardPage() {
  const pipeline = useQuery({ queryKey: ["pipeline"], queryFn: () => api.pipeline() });
  const revenue = useQuery({ queryKey: ["revenue"], queryFn: () => api.revenue() });
  const execution = useQuery({ queryKey: ["execution"], queryFn: () => api.execution() });

  const isLoading = pipeline.isLoading || revenue.isLoading || execution.isLoading;
  const error = pipeline.error ?? revenue.error ?? execution.error;

  if (isLoading) return <LoadingState label="Loading dashboard…" />;
  if (error) {
    return (
      <ErrorState
        error={error}
        onRetry={() => {
          pipeline.refetch();
          revenue.refetch();
          execution.refetch();
        }}
      />
    );
  }

  const stageFunnelData = pipeline.data
    ? Object.values(pipeline.data.stageFunnel).map((bucket) => ({ name: bucket.stage, value: bucket.value }))
    : [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold">Dashboard</h2>
        <p className="text-sm text-[var(--color-muted)]">Live snapshot across the whole pipeline and operations.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Open Pipeline Value"
          value={formatCurrency(pipeline.data?.totalPipelineValue)}
          hint={`Weighted: ${formatCurrency(pipeline.data?.weightedPipelineValue)}`}
          icon={<TrendingUp className="h-4 w-4 text-[var(--color-primary)]" />}
        />
        <KpiCard
          label="Booked Revenue"
          value={formatCurrency(revenue.data?.bookedRevenue)}
          hint="Value of Won deals"
          icon={<Banknote className="h-4 w-4 text-[var(--color-success)]" />}
        />
        <KpiCard
          label="Win Rate"
          value={formatPercent(pipeline.data?.winRate)}
          hint="Won / (Won + Dead)"
          icon={<TrendingUp className="h-4 w-4 text-[var(--color-primary)]" />}
        />
        <KpiCard
          label="Delayed Work Orders"
          value={String(execution.data?.delayed.length ?? 0)}
          hint="Overdue, not completed/recurring"
          icon={<Clock className="h-4 w-4 text-[var(--color-warning)]" />}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Pipeline value by stage</CardTitle>
        </CardHeader>
        <CardContent>
          {stageFunnelData.length === 0 ? (
            <p className="text-sm text-[var(--color-muted)]">No deals in scope.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={stageFunnelData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-20} textAnchor="end" height={70} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                <Bar dataKey="value" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      {(pipeline.data?.warnings.length ?? 0) + (revenue.data?.warnings.length ?? 0) > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4" /> Data quality notes
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            {[...(pipeline.data?.warnings ?? []), ...(revenue.data?.warnings ?? [])].map((w) => (
              <p key={w.code} className="text-sm text-[var(--color-muted)]">
                {w.message}
              </p>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
