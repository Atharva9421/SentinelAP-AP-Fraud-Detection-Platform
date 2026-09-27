import { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import type { InvoiceDto } from '../api/types';

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export function InvoicesPage() {
  const [invoices, setInvoices] = useState<InvoiceDto[]>([]);
  const [showFlaggedOnly, setShowFlaggedOnly] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<InvoiceDto[]>('/api/invoices')
      .then((res) => setInvoices(res.data))
      .finally(() => setLoading(false));
  }, []);

  const visible = showFlaggedOnly ? invoices.filter((i) => i.flagged) : invoices;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Invoices</h1>
          <p className="mt-1 text-sm text-slate-500">{invoices.length} invoices processed through the fraud pipeline.</p>
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={showFlaggedOnly}
            onChange={(e) => setShowFlaggedOnly(e.target.checked)}
            className="h-4 w-4 rounded border-slate-300"
          />
          Flagged only
        </label>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-slate-100 text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Invoice #</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Vendor</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Amount</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Issued</th>
              <th className="px-6 py-3 text-left font-medium text-slate-500">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {visible.map((invoice) => (
              <tr key={invoice.id} className={invoice.flagged ? 'bg-red-50/50 hover:bg-red-50' : 'hover:bg-slate-50'}>
                <td className="px-6 py-3 font-mono text-xs text-slate-700">{invoice.invoiceNumber}</td>
                <td className="px-6 py-3 text-slate-900">{invoice.vendorName}</td>
                <td className="px-6 py-3 text-slate-900">{currency.format(invoice.amount)}</td>
                <td className="px-6 py-3 text-slate-600">{invoice.issuedDate}</td>
                <td className="px-6 py-3">
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      invoice.flagged
                        ? 'bg-red-100 text-red-800'
                        : 'bg-slate-100 text-slate-700'
                    }`}
                  >
                    {invoice.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && visible.length === 0 && <p className="px-6 py-6 text-sm text-slate-400">No invoices found.</p>}
      </div>
    </div>
  );
}
