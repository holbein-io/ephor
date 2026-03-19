import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { dashboardService } from '../services/api';

interface SeverityHeatmapProps {
  maxNamespaces?: number;
}

const SEVERITY_LEVELS = ['critical', 'high', 'medium', 'low'] as const;
type SeverityLevel = typeof SEVERITY_LEVELS[number];

const SEVERITY_LABELS: Record<SeverityLevel, string> = {
  critical: 'Crit',
  high: 'High',
  medium: 'Med',
  low: 'Low'
};

const SEVERITY_BG: Record<SeverityLevel, (intensity: number) => string> = {
  critical: (i) => `rgba(232,97,58,${(0.08 + i * 0.17).toFixed(2)})`,
  high: (i) => `rgba(232,163,58,${(0.08 + i * 0.12).toFixed(2)})`,
  medium: (i) => `rgba(201,184,74,${(0.08 + i * 0.12).toFixed(2)})`,
  low: (i) => `rgba(91,141,239,${(0.08 + i * 0.10).toFixed(2)})`,
};

const SEVERITY_TEXT: Record<SeverityLevel, string> = {
  critical: 'var(--color-severity-critical)',
  high: 'var(--color-severity-high)',
  medium: 'var(--color-severity-medium)',
  low: 'var(--color-severity-low)',
};

export function SeverityHeatmap({ maxNamespaces = 6 }: SeverityHeatmapProps) {
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery({
    queryKey: ['namespace-comparison'],
    queryFn: () => dashboardService.getNamespaceComparison(),
    refetchInterval: 60000
  });

  const { processedData, maxValues } = useMemo(() => {
    if (!data) return { processedData: [], maxValues: {} as Record<SeverityLevel, number> };

    const sorted = [...data]
      .sort((a, b) => b.total_vulnerabilities - a.total_vulnerabilities)
      .slice(0, maxNamespaces);

    const maxVals = SEVERITY_LEVELS.reduce((acc, severity) => {
      acc[severity] = Math.max(...sorted.map(ns => ns[severity] || 0), 1);
      return acc;
    }, {} as Record<SeverityLevel, number>);

    return { processedData: sorted, maxValues: maxVals };
  }, [data, maxNamespaces]);

  const handleCellClick = (namespace: string, severity: SeverityLevel) => {
    const params = new URLSearchParams({
      namespace,
      severity: severity.toUpperCase(),
      status: 'open,triaged,accepted_risk'
    });
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
        No namespace data available
      </div>
    );
  }

  return (
    <div
      className="grid gap-[3px] text-[11px]"
      style={{ gridTemplateColumns: '76px repeat(4, 1fr)' }}
    >
      <div />
      {SEVERITY_LEVELS.map(sev => (
        <div
          key={sev}
          className="font-mono font-semibold text-text-tertiary uppercase tracking-wide text-[10px] px-2 py-1.5"
        >
          {SEVERITY_LABELS[sev]}
        </div>
      ))}

      {processedData.map(ns => (
        <div key={ns.namespace} className="contents">
          <div className="font-mono text-text-secondary text-[11px] px-2 py-2 flex items-center border-r border-border-subtle truncate">
            {ns.namespace}
          </div>
          {SEVERITY_LEVELS.map(severity => {
            const value = ns[severity] || 0;
            const intensity = value / maxValues[severity];
            return (
              <button
                key={severity}
                onClick={() => value > 0 && handleCellClick(ns.namespace, severity)}
                disabled={value === 0}
                className="py-2 text-center rounded-md font-mono font-medium transition-transform hover:scale-105 disabled:cursor-default"
                style={{
                  background: value > 0 ? SEVERITY_BG[severity](intensity) : 'var(--color-bg-tertiary)',
                  color: value > 0 ? SEVERITY_TEXT[severity] : 'var(--color-text-tertiary)',
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
