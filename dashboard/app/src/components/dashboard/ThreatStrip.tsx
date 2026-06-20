import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { DashboardMetrics } from '../../types';
import { dashboardService } from '../../services/api';

interface ThreatStripProps {
  metrics: DashboardMetrics;
}

const SEVERITY_ORDER = [
  { key: 'CRITICAL', label: 'Critical', color: 'var(--color-severity-critical)' },
  { key: 'HIGH', label: 'High', color: 'var(--color-severity-high)' },
  { key: 'MEDIUM', label: 'Medium', color: 'var(--color-severity-medium)' },
  { key: 'LOW', label: 'Low', color: 'var(--color-severity-low)' },
  { key: 'UNKNOWN', label: 'Unknown', color: 'var(--color-text-tertiary)' },
] as const;

function TrendIndicator({ delta }: { delta: number }) {
  if (delta === 0) {
    return (
      <span className="inline-flex items-center gap-1 text-text-tertiary">
        <Minus className="h-3.5 w-3.5" /> no change this week
      </span>
    );
  }
  // Growing backlog is bad (warn colour), shrinking is good (mint).
  const grew = delta > 0;
  const Icon = grew ? TrendingUp : TrendingDown;
  return (
    <span
      className="inline-flex items-center gap-1 font-medium"
      style={{ color: grew ? 'var(--color-severity-high)' : 'var(--color-accent-mint)' }}
    >
      <Icon className="h-3.5 w-3.5" />
      {grew ? '+' : '−'}{Math.abs(delta).toLocaleString()} this week
    </span>
  );
}

export function ThreatStrip({ metrics }: ThreatStripProps) {
  const sev = metrics.by_severity;
  // Total is derived from the severities shown, so the headline always reconciles
  // with the row above it. by_severity covers unresolved work (open + in-triage).
  const total = SEVERITY_ORDER.reduce((sum, s) => sum + (sev[s.key] ?? 0), 0);

  // Weekly delta: net change in unresolved vulnerabilities over the last 7 days.
  const { data: trends } = useQuery({
    queryKey: ['vuln-trends-weekly'],
    queryFn: () => dashboardService.getTrends(7),
    refetchInterval: 60000,
  });
  const weeklyDelta =
    trends && trends.length >= 2 ? trends[trends.length - 1].total - trends[0].total : null;

  return (
    <section
      className="flex items-center justify-between px-8 py-6 gap-8 border-b border-border animate-fade-up"
      style={{ background: 'linear-gradient(135deg, var(--color-bg-secondary) 0%, rgba(232,97,58,0.04) 100%)' }}
    >
      <div className="flex flex-col gap-4">
        <span className="text-[10px] font-bold tracking-[0.14em] uppercase text-text-tertiary">
          Security Posture
        </span>

        <div className="flex gap-10">
          {SEVERITY_ORDER.map(s => (
            <div key={s.key} className="min-w-[60px]">
              <div
                className="font-mono text-[32px] font-medium leading-none animate-count-up"
                style={{ color: s.color }}
              >
                {(sev[s.key] ?? 0).toLocaleString()}
              </div>
              <div className="text-[10px] font-bold tracking-[0.1em] uppercase text-text-tertiary mt-2">
                {s.label}
              </div>
            </div>
          ))}
        </div>

        <p className="flex items-center gap-2 text-[13px] text-text-secondary">
          <span>
            <span className="font-mono font-medium text-text-primary">{total.toLocaleString()}</span>{' '}
            unresolved vulnerabilities
          </span>
          {weeklyDelta !== null && (
            <>
              <span className="text-text-tertiary">&middot;</span>
              <TrendIndicator delta={weeklyDelta} />
            </>
          )}
        </p>
      </div>

      <Link
        to="/vulnerabilities"
        className="shrink-0 px-5 py-2.5 rounded-[10px] text-[13px] font-semibold bg-accent text-white shadow-[0_4px_20px_rgba(232,97,58,0.25)] hover:shadow-[0_6px_28px_rgba(232,97,58,0.4)] hover:-translate-y-px transition-all no-underline"
      >
        View all
      </Link>
    </section>
  );
}
