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

const SEVERITY_COLORS: Record<SeverityLevel, { bg: string; text: string; border: string }> = {
  critical: { bg: 'bg-severity-critical', text: 'text-white', border: 'border-severity-critical' },
  high: { bg: 'bg-severity-high', text: 'text-bg-primary', border: 'border-severity-high' },
  medium: { bg: 'bg-severity-medium', text: 'text-bg-primary', border: 'border-severity-medium' },
  low: { bg: 'bg-severity-low', text: 'text-white', border: 'border-severity-low' }
};

const SEVERITY_LABELS: Record<SeverityLevel, string> = {
  critical: 'Critical',
  high: 'High',
  medium: 'Medium',
  low: 'Low'
};

function getIntensityClass(value: number, maxValue: number): string {
  if (value === 0) return 'bg-bg-tertiary';
  const ratio = value / maxValue;
  if (ratio >= 0.75) return 'opacity-100';
  if (ratio >= 0.5) return 'opacity-80';
  if (ratio >= 0.25) return 'opacity-60';
  return 'opacity-40';
}

export function SeverityHeatmap({ maxNamespaces = 10 }: SeverityHeatmapProps) {
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery({
    queryKey: ['namespace-comparison'],
    queryFn: () => dashboardService.getNamespaceComparison(),
    refetchInterval: 60000
  });

  const { processedData, maxValues } = useMemo(() => {
    if (!data) return { processedData: [], maxValues: {} as Record<SeverityLevel, number> };

    // Sort by total vulnerabilities and take top N
    const sorted = [...data]
      .sort((a, b) => b.total_vulnerabilities - a.total_vulnerabilities)
      .slice(0, maxNamespaces);

    // Calculate max for each severity for intensity scaling
    const maxVals = SEVERITY_LEVELS.reduce((acc, severity) => {
      acc[severity] = Math.max(...sorted.map(ns => ns[severity] || 0), 1);
      return acc;
    }, {} as Record<SeverityLevel, number>);

    return { processedData: sorted, maxValues: maxVals };
  }, [data, maxNamespaces]);

  const handleCellClick = (namespace: string, severity: SeverityLevel) => {
    // Navigate to vulnerabilities page with filters
    // Note: Status filter matches API query (open, triaged, accepted_risk)
    const params = new URLSearchParams({
      namespace,
      severity: severity.toUpperCase(),
      status: 'open,triaged,accepted_risk'
    });
    navigate(`/vulnerabilities?${params.toString()}`);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-accent" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="text-center py-8 text-text-tertiary">
        Failed to load namespace data
      </div>
    );
  }

  if (processedData.length === 0) {
    return (
      <div className="text-center py-8 text-text-tertiary">
        No vulnerability data available
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className="text-left text-xs font-medium text-text-tertiary uppercase tracking-wider p-2 min-w-[150px]">
              Namespace
            </th>
            {SEVERITY_LEVELS.map(severity => (
              <th
                key={severity}
                className="text-center text-xs font-medium text-text-tertiary uppercase tracking-wider p-2 w-24"
              >
                <div className="flex items-center justify-center gap-1">
                  <span
                    className={`w-2 h-2 rounded-full ${SEVERITY_COLORS[severity].bg}`}
                  />
                  {SEVERITY_LABELS[severity]}
                </div>
              </th>
            ))}
            <th className="text-center text-xs font-medium text-text-tertiary uppercase tracking-wider p-2 w-24">
              Total
            </th>
          </tr>
        </thead>
        <tbody>
          {processedData.map((ns, idx) => (
            <tr
              key={ns.namespace}
              className={idx % 2 === 0 ? 'bg-bg-card' : 'bg-bg-secondary'}
            >
              <td className="p-2">
                <div className="flex flex-col">
                  <span className="text-sm font-medium text-text-primary truncate max-w-[150px]" title={ns.namespace}>
                    {ns.namespace}
                  </span>
                  <span className="text-xs text-text-tertiary">
                    {ns.open} open, {ns.resolved} resolved
                  </span>
                </div>
              </td>
              {SEVERITY_LEVELS.map(severity => {
                const value = ns[severity] || 0;
                return (
                  <td key={severity} className="p-1">
                    <HeatmapCell
                      value={value}
                      severity={severity}
                      maxValue={maxValues[severity]}
                      onClick={() => value > 0 && handleCellClick(ns.namespace, severity)}
                    />
                  </td>
                );
              })}
              <td className="p-2 text-center">
                <span className="text-sm font-bold text-text-primary">
                  {ns.total_vulnerabilities}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Legend */}
      <div className="mt-4 flex items-center justify-between text-xs text-text-tertiary">
        <div className="flex items-center gap-4">
          <span>Intensity:</span>
          <div className="flex items-center gap-1">
            <div className="w-4 h-4 bg-bg-tertiary rounded border border-border" />
            <span>None</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-4 h-4 bg-severity-critical opacity-40 rounded" />
            <span>Low</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-4 h-4 bg-severity-critical rounded" />
            <span>High</span>
          </div>
        </div>
        <span className="text-text-tertiary">Click cells to filter vulnerabilities</span>
      </div>
    </div>
  );
}

interface HeatmapCellProps {
  value: number;
  severity: SeverityLevel;
  maxValue: number;
  onClick: () => void;
}

function HeatmapCell({ value, severity, maxValue, onClick }: HeatmapCellProps) {
  const colors = SEVERITY_COLORS[severity];
  const intensityClass = getIntensityClass(value, maxValue);
  const hasValue = value > 0;

  return (
    <button
      onClick={onClick}
      disabled={!hasValue}
      className={`
        w-full h-12 rounded-md flex items-center justify-center
        transition-all duration-150 border
        ${hasValue
          ? `${colors.bg} ${intensityClass} ${colors.border} cursor-pointer hover:scale-105 hover:shadow-md`
          : 'bg-bg-tertiary border-border cursor-default'
        }
      `}
      title={hasValue ? `${value} ${severity} vulnerabilities - Click to view` : 'No vulnerabilities'}
    >
      {hasValue ? (
        <span className={`text-sm font-bold ${colors.text}`}>
          {value}
        </span>
      ) : (
        <span className="text-text-tertiary text-xs">-</span>
      )}
    </button>
  );
}
