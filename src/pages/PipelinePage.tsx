import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { formatCurrency, formatPercent } from "@/lib/utils";
import { KpiCard } from "@/components/KpiCard";
import { LoadingState } from "@/components/LoadingState";
import { ErrorState } from "@/components/ErrorState";
import { WarningsList } from "@/components/WarningsList";
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

function thisFiscalQuarter(): { start: string; end: string } {
  const now = new Date();
  const fyStartYear = now.getMonth() + 1 >= 4 ? now.getFullYear() : now.getFullYear() - 1;
  const monthsSinceFyStart = (now.getMonth() - 3 + 12) % 12;
  const quarterIndex = Math.floor(monthsSinceFyStart / 3);
  const startMonth = 3 + quarterIndex * 3; // 0-indexed
  const start = new Date(fyStartYear, startMonth, 1);
  const end = new Date(fyStartYear, startMonth + 3, 0);
  const iso = (d: Date) => d.toISOString().slice(0, 10);
  return { start: iso(start), end: iso(end) };
}

export function PipelinePage() {
  const pipeline = useQuery({ queryKey: ["pipeline"], queryFn: () => api.pipeline() });
  const horizon = thisFiscalQuarter();
  const forecast = useQuery({
    queryKey: ["forecast", horizon.start, horizon.end],
    queryFn: () => api.forecast(horizon.start, horizon.end),
  });

  if (pipeline.isLoading || forecast.isLoading) return <LoadingState label="Loading pipeline…" />;
  const error = pipeline.error ?? forecast.error;
  if (error) {
    return <ErrorState error={error} onRetry={() => { pipeline.refetch(); forecast.refetch(); }} />;
  }

  const stageFunnelData = pipeline.data
    ? Object.values(pipeline.data.stageFunnel).map((bucket) => ({ name: bucket.stage, count: bucket.count, value: bucket.value }))
    : [];
  const agingData = pipeline.data?.agingBuckets.map((b) => ({ name: b.label, value: b.value })) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold">Pipeline &amp; Forecast</h2>
        <p className="text-sm text-[var(--color-muted)]">Open deal pipeline shape and this quarter's weighted forecast.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard label="Total Pipeline Value" value={formatCurrency(pipeline.data?.totalPipelineValue)} />
        <KpiCard label="Average Deal Size" value={formatCurrency(pipeline.data?.averageDealSize)} />
        <KpiCard label="Win Rate" value={formatPercent(pipeline.data?.winRate)} />
        <KpiCard
          label="This Quarter Forecast"
          value={formatCurrency(forecast.data?.weightedForecast)}
          hint={`Best case ${formatCurrency(forecast.data?.bestCase)} / Worst case ${formatCurrency(forecast.data?.worstCase)}`}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Deal count by stage</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={stageFunnelData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-25} textAnchor="end" height={80} />
                <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Open deal aging</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={agingData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                <Bar dataKey="value" fill="var(--color-warning)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      <WarningsList warnings={[...(pipeline.data?.warnings ?? []), ...(forecast.data?.warnings ?? [])]} />
    </div>
  );
}
