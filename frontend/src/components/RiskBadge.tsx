export function RiskBadge({ score }: { score: number }) {
  let style = 'bg-emerald-100 text-emerald-800 ring-emerald-300';
  if (score >= 80) style = 'bg-red-100 text-red-800 ring-red-300';
  else if (score >= 50) style = 'bg-orange-100 text-orange-800 ring-orange-300';
  else if (score >= 25) style = 'bg-amber-100 text-amber-800 ring-amber-300';

  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${style}`}>
      {score.toFixed(0)}
    </span>
  );
}
