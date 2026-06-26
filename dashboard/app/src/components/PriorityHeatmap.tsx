import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { dashboardService } from '../services/api';
import { PRIORITY_TIER_COLORS } from '../constants/colors';

interface PriorityHeatmapProps {
  maxNamespaces?: number;
}

const TIERS = ['p0', 'p1', 'p2'] as const;
type Tier = typeof TIERS[number];

const TIER_LABELS: Record<Tier, 'P0' | 'P1' | 'P2'> = { p0: 'P0', p1: 'P1', p2: 'P2' };

const TIER_BG: Record<Tier, (intensity: number) => string> = {
  p0: (i) => `rgba(232,97,58,${(0.08 + i * 0.17).toFixed(2)})`,
  p1: (i) => `rgba(232,163,58,${(0.08 + i * 0.12).toFixed(2)})`,
  p2: (i) => `rgba(201,184,74,${(0.08 + i * 0.12).toFixed(2)})`,
};

const TIER_TEXT: Record<Tier, string> = {
  p0: 'var(--color-severity-critical)',
  p1: 'var(--color-severity-high)',
  p2: 'var(--color-severity-medium)',
};

export function PriorityHeatmap({ maxNamespaces = 6 }: PriorityHeatmapProps) {
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery({
    queryKey: ['namespace-priority'],
    queryFn: () => dashboardService.getNamespacePriority(),
    refetchInterval: 60000
  });

  const { processedData, maxValues } = useMemo(() => {
    if (!data) return { processedData: [], maxValues: {} as Record<Tier, number> };

    const actionTotal = (ns: typeof data[number]) => ns.p0 * 1_000_000 + ns.p1 * 1_000 + ns.p2;

    const sorted = [...data]
      .filter(ns => ns.p0 + ns.p1 + ns.p2 > 0)
      .sort((a, b) => actionTotal(b) - actionTotal(a))
      .slice(0, maxNamespaces);

    const maxVals = TIERS.reduce((acc, tier) => {
      acc[tier] = Math.max(...sorted.map(ns => ns[tier] || 0), 1);
      return acc;
    }, {} as Record<Tier, number>);

    return { processedData: sorted, maxValues: maxVals };
  }, [data, maxNamespaces]);

  const handleCellClick = (namespace: string) => {
    const params = new URLSearchParams({ namespace, sort_by: 'priority' });
    navigate(`/vulnerabilities?${params.toString()}`);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-40">
        <Loader2 className="h-6 w-6 animate-spin text-accent" />
      </div>
    );
  }

  if (error || !data || processedData.length === 0) {
    return (
      <div className="text-center py-6 text-text-tertiary text-sm">
        No actionable findings across namespaces
      </div>
    );
  }

  return (
    <div
      className="grid gap-[3px] text-[11px]"
      style={{ gridTemplateColumns: '120px repeat(3, 1fr)' }}
    >
      <div />
      {TIERS.map(tier => (
        <div
          key={tier}
          title={PRIORITY_TIER_COLORS[TIER_LABELS[tier]].title}
          className="font-mono font-semibold text-text-tertiary uppercase tracking-wide text-[10px] px-2 py-1.5"
        >
          {TIER_LABELS[tier]}
        </div>
      ))}

      {processedData.map(ns => (
        <div key={ns.namespace} className="contents">
          <div className="font-mono text-text-secondary text-[11px] px-2 py-2 flex items-center border-r border-border-subtle truncate">
            {ns.namespace}
          </div>
          {TIERS.map(tier => {
            const value = ns[tier] || 0;
            const intensity = value / maxValues[tier];
            return (
              <button
                key={tier}
                onClick={() => value > 0 && handleCellClick(ns.namespace)}
                disabled={value === 0}
                className="py-2 text-center rounded-md font-mono font-medium transition-transform hover:scale-105 disabled:cursor-default"
                style={{
                  background: value > 0 ? TIER_BG[tier](intensity) : 'var(--color-bg-tertiary)',
                  color: value > 0 ? TIER_TEXT[tier] : 'var(--color-text-tertiary)',
                }}
              >
                {value > 0 ? value : '-'}
              </button>
            );
          })}
        </div>
      ))}
    </div>
  );
}
