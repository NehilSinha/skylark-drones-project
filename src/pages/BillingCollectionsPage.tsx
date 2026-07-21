import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import { KpiCard } from "@/components/KpiCard";
import { LoadingState } from "@/components/LoadingState";
import { ErrorState } from "@/components/ErrorState";
import { WarningsList } from "@/components/WarningsList";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export function BillingCollectionsPage() {
  const billing = useQuery({ queryKey: ["billing"], queryFn: () => api.billing() });
  const collections = useQuery({ queryKey: ["collections"], queryFn: () => api.collections() });

  if (billing.isLoading || collections.isLoading) return <LoadingState label="Loading billing & collections…" />;
  const error = billing.error ?? collections.error;
  if (error) {
    return <ErrorState error={error} onRetry={() => { billing.refetch(); collections.refetch(); }} />;
  }

  const billingStatusData = Object.entries(billing.data?.billingStatusDistribution ?? {}).map(([name, value]) => ({ name, value }));

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold">Billing &amp; Collections</h2>
        <p className="text-sm text-[var(--color-muted)]">Amounts to be billed, collected, and outstanding receivables.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <KpiCard label="Amount to be Billed" value={formatCurrency(billing.data?.totalAmountToBeBilled)} />
        <KpiCard label="Total Collected" value={formatCurrency(collections.data?.totalCollected)} />
        <KpiCard label="Total Receivable" value={formatCurrency(collections.data?.totalReceivable)} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Billing status distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={billingStatusData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-20} textAnchor="end" height={70} />
                <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="value" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Aged receivables</CardTitle>
          </CardHeader>
          <CardContent>
            {collections.data?.agedReceivables.length === 0 ? (
              <p className="text-sm text-[var(--color-muted)]">No outstanding receivables.</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[var(--color-muted)]">
                    <th className="pb-2">Deal</th>
                    <th className="pb-2 text-right">Amount</th>
                    <th className="pb-2 text-right">Days</th>
                  </tr>
                </thead>
                <tbody>
                  {collections.data?.agedReceivables.slice(0, 10).map((r) => (
                    <tr key={r.dealName} className="border-t border-[var(--color-border)]">
                      <td className="py-2">{r.dealName}</td>
                      <td className="py-2 text-right">{formatCurrency(r.amount)}</td>
                      <td className="py-2 text-right text-[var(--color-warning)]">{r.daysSinceProbableEnd}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>
      </div>

      <WarningsList warnings={[...(billing.data?.warnings ?? []), ...(collections.data?.warnings ?? [])]} />
    </div>
  );
}
