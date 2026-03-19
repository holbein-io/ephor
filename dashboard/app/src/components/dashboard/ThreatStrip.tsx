import { Link } from 'react-router-dom';
import { DashboardMetrics } from '../../types';

interface ThreatStripProps {
  metrics: DashboardMetrics;
}

export function ThreatStrip({ metrics }: ThreatStripProps) {
  const { by_severity: sev, by_status: status, total_vulnerabilities: total } = metrics;
  const sum = sev.CRITICAL + sev.HIGH + sev.MEDIUM + sev.LOW || 1;

  const critPct = (sev.CRITICAL / sum) * 100;
  const highPct = critPct + (sev.HIGH / sum) * 100;
  const medPct = highPct + (sev.MEDIUM / sum) * 100;

  const conicGradient = `conic-gradient(
    var(--color-severity-critical) 0% ${critPct}%,
    var(--color-severity-high) ${critPct}% ${highPct}%,
    var(--color-severity-medium) ${highPct}% ${medPct}%,
    var(--color-severity-low) ${medPct}% 100%
  )`;

  const kpis = [
    { label: 'Critical', value: sev.CRITICAL, color: 'var(--color-severity-critical)' },
    { label: 'High', value: sev.HIGH, color: 'var(--color-severity-high)' },
    { label: 'Open', value: status.open, color: 'var(--color-accent-cool)' },
    { label: 'Resolved', value: status.resolved, color: 'var(--color-accent-mint)' },
  ];

  return (
    <section
      className="grid items-center px-8 py-6 gap-8 border-b border-border animate-fade-up"
      style={{
        gridTemplateColumns: '1fr auto 1fr',
        background: 'linear-gradient(135deg, var(--color-bg-secondary) 0%, rgba(232,97,58,0.04) 100%)',
      }}
    >
      <div className="flex items-center gap-5">
        <div
          className="w-[72px] h-[72px] rounded-full p-1.5 flex-shrink-0"
          style={{ background: conicGradient }}
        >
          <div className="w-full h-full rounded-full bg-bg-primary flex flex-col items-center justify-center">
            <span className="font-display text-[22px] leading-none text-text-primary">
              {total.toLocaleString()}
            </span>
            <span className="text-[7px] font-bold tracking-[0.1em] uppercase text-text-tertiary mt-px">
              Total
            </span>
          </div>
        </div>
        <div>
          <h1 className="font-display text-[28px] italic text-text-primary leading-tight">
            Security Posture
          </h1>
          <p className="text-[13px] text-text-secondary mt-1">
            {total.toLocaleString()} findings across active workloads
          </p>
        </div>
      </div>

      <div className="flex gap-8">
        {kpis.map(kpi => (
          <div key={kpi.label} className="text-center min-w-[80px]">
            <div
              className="font-mono text-[28px] font-medium leading-none animate-count-up"
              style={{ color: kpi.color }}
            >
              {kpi.value.toLocaleString()}
            </div>
            <div className="text-[10px] font-bold tracking-[0.1em] uppercase text-text-tertiary mt-1.5">
              {kpi.label}
            </div>
          </div>
        ))}
      </div>

      <div className="flex gap-3 justify-end">
        <button className="px-5 py-2.5 rounded-[10px] text-[13px] font-semibold bg-bg-tertiary text-text-secondary border border-border hover:border-accent hover:text-accent transition-all">
          Export Report
        </button>
        <Link
          to="/vulnerabilities"
          className="px-5 py-2.5 rounded-[10px] text-[13px] font-semibold bg-accent text-white shadow-[0_4px_20px_rgba(232,97,58,0.25)] hover:shadow-[0_6px_28px_rgba(232,97,58,0.4)] hover:-translate-y-px transition-all no-underline"
        >
          View All
        </Link>
      </div>
    </section>
  );
}
