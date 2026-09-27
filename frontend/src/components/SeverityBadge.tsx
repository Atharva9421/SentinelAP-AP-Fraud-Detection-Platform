import type { Severity } from '../api/types';

const STYLES: Record<Severity, string> = {
  LOW: 'bg-slate-100 text-slate-700 ring-slate-300',
  MEDIUM: 'bg-amber-100 text-amber-800 ring-amber-300',
  HIGH: 'bg-orange-100 text-orange-800 ring-orange-300',
  CRITICAL: 'bg-red-100 text-red-800 ring-red-300',
};

export function SeverityBadge({ severity }: { severity: Severity }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${STYLES[severity]}`}>
      {severity}
    </span>
  );
}
