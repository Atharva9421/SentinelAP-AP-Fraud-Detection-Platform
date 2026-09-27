interface StatCardProps {
  label: string;
  value: string;
  tone?: 'default' | 'danger' | 'warning';
}

const TONE_STYLES: Record<NonNullable<StatCardProps['tone']>, string> = {
  default: 'text-slate-900',
  danger: 'text-red-600',
  warning: 'text-amber-600',
};

export function StatCard({ label, value, tone = 'default' }: StatCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className={`mt-2 text-3xl font-semibold ${TONE_STYLES[tone]}`}>{value}</p>
    </div>
  );
}
