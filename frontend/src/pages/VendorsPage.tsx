import { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import type { VendorDto } from '../api/types';
import { RiskBadge } from '../components/RiskBadge';

export function VendorsPage() {
  const [vendors, setVendors] = useState<VendorDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<VendorDto[]>('/api/vendors')
      .then((res) => setVendors(res.data.sort((a, b) => b.riskScore - a.riskScore)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Vendors</h1>
        <p className="mt-1 text-sm text-slate-500">{vendors.length} vendors tracked, ranked by current risk score.</p>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-slate-100 text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Vendor</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Source</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Bank Acct</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Invoices</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Open Alerts</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Risk Score</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {vendors.map((vendor) => (
              <tr key={vendor.id} className="hover:bg-slate-50">
                <td className="px-6 py-3">
                  <p className="font-medium text-slate-900">{vendor.name}</p>
                  <p className="text-xs text-slate-400">{vendor.email}</p>
                </td>
                <td className="px-6 py-3 text-slate-600">{vendor.source}</td>
                <td className="px-6 py-3 text-slate-600">•••• {vendor.bankAccountLast4 ?? '—'}</td>
                <td className="px-6 py-3 text-slate-600">{vendor.invoiceCount}</td>
                <td className="px-6 py-3">
                  {vendor.openAlertCount > 0 ? (
                    <span className="font-medium text-red-600">{vendor.openAlertCount}</span>
                  ) : (
                    <span className="text-slate-400">0</span>
                  )}
                </td>
                <td className="px-6 py-3">
                  <RiskBadge score={vendor.riskScore} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && vendors.length === 0 && <p className="px-6 py-6 text-sm text-slate-400">No vendors found.</p>}
      </div>
    </div>
  );
}
