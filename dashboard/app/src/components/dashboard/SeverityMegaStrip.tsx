interface SeverityMegaStripProps {
  severity: { CRITICAL: number; HIGH: number; MEDIUM: number; LOW: number; UNKNOWN: number };
  total: number;
}

const segments = [
  { key: 'CRITICAL' as const, label: 'Critical', color: 'var(--color-severity-critical)' },
  { key: 'HIGH' as const, label: 'High', color: 'var(--color-severity-high)' },
  { key: 'MEDIUM' as const, label: 'Medium', color: 'var(--color-severity-medium)' },
  { key: 'LOW' as const, label: 'Low', color: 'var(--color-severity-low)' },
];

export function SeverityMegaStrip({ severity, total }: SeverityMegaStripProps) {
  const safeTotal = total || 1;

  return (
    <div className="col-span-12 bg-bg-secondary border border-border rounded-2xl px-[22px] py-[18px] animate-fade-up">
      <div className="flex items-center justify-between mb-3">
        <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
          Severity Distribution
        </span>
        <div className="flex gap-4">
          {segments.map(seg => {
            const count = severity[seg.key];
            const pct = ((count / safeTotal) * 100).toFixed(1);
            return (
              <span key={seg.key} className="flex items-center gap-[5px] font-mono text-[11px] text-text-secondary">
                <span className="w-2 h-2 rounded-sm" style={{ background: seg.color }} />
                {seg.label} {count} ({pct}%)
              </span>
            );
          })}
        </div>
      </div>
      <div
        className="w-full h-3 rounded-full bg-white/[0.03] flex overflow-hidden"
        style={{ animation: 'barReveal 1s ease-out 0.3s both' }}
      >
        {segments.map(seg => {
          const pct = (severity[seg.key] / safeTotal) * 100;
          if (pct === 0) return null;
          return (
            <div
              key={seg.key}
              className="h-full"
              style={{ width: `${pct}%`, background: seg.color }}
            />
          );
        })}
      </div>
    </div>
  );
}
