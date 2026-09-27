import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { apiClient } from '../api/client';
import type { DashboardSummaryDto } from '../api/types';
import { StatCard } from '../components/StatCard';
import { SeverityBadge } from '../components/SeverityBadge';
import { RiskBadge } from '../components/RiskBadge';

const ALERT_TYPE_LABELS: Record<string, string> = {
  BANK_ACCOUNT_CHANGE: 'Bank Account Change',
  DUPLICATE_INVOICE: 'Duplicate Invoice',
  VELOCITY_ANOMALY: 'Velocity Anomaly',
  ML_ANOMALY: 'ML Anomaly',
};

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummaryDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient
      .get<DashboardSummaryDto>('/api/dashboard/summary')
      .then((res) => setSummary(res.data))
      .catch(() => setError('Could not load dashboard data. Is the backend running?'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-sm text-slate-500">Loading dashboard...</p>;
  if (error) return <p className="text-sm text-red-600">{error}</p>;
  if (!summary) return null;

  const chartData = Object.entries(summary.alertsByType).map(([type, count]) => ({
    type: ALERT_TYPE_LABELS[type] ?? type,
    count,
  }));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Dashboard</h1>
        <p className="mt-1 text-sm text-slate-500">Live view of AP exposure, vendor risk, and open fraud alerts.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total AP Exposure" value={currency.format(summary.totalApExposure)} />
        <StatCard label="Open Alerts" value={String(summary.openAlerts)} tone={summary.openAlerts > 0 ? 'warning' : 'default'} />
        <StatCard label="Critical Alerts" value={String(summary.criticalAlerts)} tone={summary.criticalAlerts > 0 ? 'danger' : 'default'} />
        <StatCard label="Avg. Vendor Risk Score" value={summary.averageRiskScore.toFixed(1)} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Alerts by Type</h2>
          {chartData.length === 0 ? (
            <p className="text-sm text-slate-400">No alerts yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={chartData} margin={{ top: 4, right: 8, left: -16, bottom: 4 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="type" tick={{ fontSize: 11, fill: '#64748b' }} interval={0} angle={-15} textAnchor="end" height={60} />
                <YAxis tick={{ fontSize: 11, fill: '#64748b' }} allowDecimals={false} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="count" fill="#0f172a" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Portfolio Snapshot</h2>
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <dt className="text-slate-500">Total vendors tracked</dt>
              <dd className="font-medium text-slate-900">{summary.totalVendors}</dd>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <dt className="text-slate-500">Total invoices processed</dt>
              <dd className="font-medium text-slate-900">{summary.totalInvoices}</dd>
            </div>
            <div className="flex justify-between pb-2">
              <dt className="text-slate-500">Alert-to-invoice ratio</dt>
              <dd className="font-medium text-slate-900">
                {summary.totalInvoices > 0
                  ? `${((summary.openAlerts / summary.totalInvoices) * 100).toFixed(1)}%`
                  : '—'}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-4">
          <h2 className="text-sm font-semibold text-slate-700">Most Recent Alerts</h2>
        </div>
        <ul className="divide-y divide-slate-100">
          {summary.recentAlerts.map((alert) => (
            <li key={alert.id} className="flex items-center justify-between px-6 py-4">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-slate-900">{alert.vendorName}</p>
                <p className="truncate text-xs text-slate-500">{alert.reason}</p>
              </div>
              <div className="ml-4 flex shrink-0 items-center gap-3">
                <RiskBadge score={alert.riskScore} />
                <SeverityBadge severity={alert.severity} />
              </div>
            </li>
          ))}
          {summary.recentAlerts.length === 0 && (
            <li className="px-6 py-6 text-sm text-slate-400">No alerts recorded yet.</li>
          )}
        </ul>
      </div>
    </div>
  );
}
