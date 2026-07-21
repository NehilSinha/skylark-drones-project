import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { KpiCard } from "@/components/KpiCard";
import { LoadingState } from "@/components/LoadingState";
import { ErrorState } from "@/components/ErrorState";
import { WarningsList } from "@/components/WarningsList";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

const PIE_COLORS = ["#2563eb", "#16a34a", "#d97706", "#dc2626", "#7c3aed", "#0891b2", "#64748b"];

export function ExecutionPage() {
  const execution = useQuery({ queryKey: ["execution"], queryFn: () => api.execution() });

  if (execution.isLoading) return <LoadingState label="Loading execution status…" />;
  if (execution.error) return <ErrorState error={execution.error} onRetry={() => execution.refetch()} />;

  const statusData = Object.entries(execution.data?.statusDistribution ?? {}).map(([name, value]) => ({ name, value }));

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold">Execution</h2>
        <p className="text-sm text-[var(--color-muted)]">Work order execution status and delivery performance.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <KpiCard label="Delayed Work Orders" value={String(execution.data?.delayed.length ?? 0)} />
        <KpiCard
          label="Avg. Delivery Variance"
          value={
            execution.data?.averageDeliveryVarianceDays != null
              ? `${execution.data.averageDeliveryVarianceDays.toFixed(1)} days`
              : "—"
          }
          hint="Data delivery date vs. probable end date"
        />
        <KpiCard label="Total Work Orders" value={String(statusData.reduce((sum, s) => sum + s.value, 0))} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Status distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={statusData} dataKey="value" nameKey="name" outerRadius={100} label={(entry) => entry.name}>
                  {statusData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Delayed work orders</CardTitle>
          </CardHeader>
          <CardContent>
            {execution.data?.delayed.length === 0 ? (
              <p className="text-sm text-[var(--color-muted)]">Nothing overdue.</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[var(--color-muted)]">
                    <th className="pb-2">Deal</th>
                    <th className="pb-2">Customer</th>
                    <th className="pb-2 text-right">Days overdue</th>
                  </tr>
                </thead>
                <tbody>
                  {execution.data?.delayed.map((wo) => (
                    <tr key={wo.dealName} className="border-t border-[var(--color-border)]">
                      <td className="py-2">{wo.dealName}</td>
                      <td className="py-2">{wo.customerCode}</td>
                      <td className="py-2 text-right text-[var(--color-danger)]">{wo.daysOverdue}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>
      </div>

      <WarningsList warnings={execution.data?.warnings ?? []} />
    </div>
  );
}
