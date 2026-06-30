import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { DashboardMetrics } from '../../types';
import { dashboardService } from '../../services/api';
import { PRIORITY_TIER_COLORS } from '../../constants/colors';

interface ThreatStripProps {
  metrics: DashboardMetrics;
}

const TIERS = [
  { key: 'P0', color: 'var(--color-severity-critical)' },
  { key: 'P1', color: 'var(--color-severity-high)' },
  { key: 'P2', color: 'var(--color-severity-medium)' },
] as const;

function TrendIndicator({ delta }: { delta: number }) {
  if (delta === 0) {
    return (
      <span className="inline-flex items-center gap-1 text-text-tertiary">
        <Minus className="h-3.5 w-3.5" /> no change this week
      </span>
    );
  }
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
  const actionNow = metrics.action_now;
  const backlog = metrics.total_active_vulnerabilities;

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
          Priority Worklist
        </span>

        <div className="flex items-baseline gap-3">
          <span
            className="font-mono text-[44px] font-medium leading-none animate-count-up shrink-0"
            style={{ color: actionNow > 0 ? 'var(--color-severity-critical)' : 'var(--color-accent-mint)' }}
          >
            {actionNow.toLocaleString()}
          </span>
          <span className="text-[15px] text-text-secondary whitespace-nowrap">
            {actionNow === 1 ? 'CVE needs action now' : 'CVEs need action now'}
          </span>
        </div>

        <div className="flex flex-wrap gap-2">
          {TIERS.map(t => (
            <span
              key={t.key}
              title={PRIORITY_TIER_COLORS[t.key].title}
              className="inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-[12px] font-mono"
              style={{ borderColor: 'var(--color-border)' }}
            >
              <span className="font-semibold" style={{ color: t.color }}>{t.key}</span>
              <span className="text-text-primary">{(metrics.by_priority[t.key] ?? 0).toLocaleString()}</span>
            </span>
          ))}
        </div>

        <p className="flex items-center gap-2 text-[13px] text-text-secondary">
          <span>
            total backlog{' '}
            <span className="font-mono font-medium text-text-primary">{backlog.toLocaleString()}</span>
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
        View worklist
      </Link>
    </section>
  );
}
