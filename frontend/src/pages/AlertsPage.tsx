import { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import type { AlertDto, AlertStatus } from '../api/types';
import { SeverityBadge } from '../components/SeverityBadge';
import { RiskBadge } from '../components/RiskBadge';

const STATUS_OPTIONS: AlertStatus[] = ['OPEN', 'REVIEWING', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE', 'RESOLVED'];

const ALERT_TYPE_LABELS: Record<string, string> = {
  BANK_ACCOUNT_CHANGE: 'Bank Account Change',
  DUPLICATE_INVOICE: 'Duplicate Invoice',
  VELOCITY_ANOMALY: 'Velocity Anomaly',
  ML_ANOMALY: 'ML Anomaly',
};

export function AlertsPage() {
  const [alerts, setAlerts] = useState<AlertDto[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const loadAlerts = (status: string) => {
    setLoading(true);
    apiClient
      .get<AlertDto[]>('/api/alerts', { params: status ? { status } : {} })
      .then((res) => setAlerts(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadAlerts(statusFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const handleStatusChange = async (alertId: string, newStatus: AlertStatus) => {
    setUpdatingId(alertId);
    try {
      const { data } = await apiClient.patch<AlertDto>(`/api/alerts/${alertId}/status`, { status: newStatus });
      setAlerts((prev) => prev.map((a) => (a.id === alertId ? data : a)));
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Fraud Alerts</h1>
          <p className="mt-1 text-sm text-slate-500">Every alert raised by the rules engine and the ML anomaly model.</p>
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s.replace('_', ' ')}
            </option>
          ))}
        </select>
      </div>

      <div className="space-y-3">
        {alerts.map((alert) => (
          <div key={alert.id} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="mb-2 flex items-center gap-2">
                  <SeverityBadge severity={alert.severity} />
                  <RiskBadge score={alert.riskScore} />
                  <span className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    {ALERT_TYPE_LABELS[alert.alertType] ?? alert.alertType}
                  </span>
                </div>
                <p className="font-medium text-slate-900">
                  {alert.vendorName}
                  {alert.invoiceNumber && <span className="ml-2 font-mono text-xs text-slate-400">{alert.invoiceNumber}</span>}
                </p>
                <p className="mt-1 text-sm text-slate-600">{alert.reason}</p>
                <p className="mt-2 text-xs text-slate-400">{new Date(alert.createdAt).toLocaleString()}</p>
              </div>
              <select
                value={alert.status}
                disabled={updatingId === alert.id}
                onChange={(e) => handleStatusChange(alert.id, e.target.value as AlertStatus)}
                className="shrink-0 rounded-lg border border-slate-300 px-2 py-1.5 text-xs font-medium disabled:opacity-50"
              >
                {STATUS_OPTIONS.map((s) => (
                  <option key={s} value={s}>
                    {s.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </div>
          </div>
        ))}
        {!loading && alerts.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-300 px-6 py-10 text-center text-sm text-slate-400">
            No alerts match this filter.
          </p>
        )}
      </div>
    </div>
  );
}
