import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ChevronDown, ChevronUp } from 'lucide-react';
import { sbomService } from '../../services/api';
import { cn } from '../../utils';

export function PreScanAlerts() {
  const [expanded, setExpanded] = useState(false);

  const { data: alertCount, isLoading: isLoadingCount } = useQuery({
    queryKey: ['pre-scan-alert-count'],
    queryFn: () => sbomService.getPreScanAlertCount(),
    refetchInterval: 60000,
  });

  const { data: alerts, isLoading: isLoadingAlerts } = useQuery({
    queryKey: ['pre-scan-alerts'],
    queryFn: () => sbomService.getPreScanAlerts(20),
    enabled: expanded,
  });

  if (isLoadingCount) {
    return (
      <div className="flex flex-col gap-2 animate-pulse">
        <div className="h-8 bg-bg-tertiary rounded-lg" />
      </div>
    );
  }

  const count = alertCount?.count ?? 0;

  if (count === 0) {
    return (
      <div className="flex items-center gap-2.5 text-sm text-text-tertiary">
        <AlertTriangle className="h-4 w-4" />
        <span>No pre-scan alerts</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2.5">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center justify-between w-full text-left"
      >
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-severity-high/10 flex items-center justify-center">
            <AlertTriangle className="h-4 w-4 text-severity-high" />
          </div>
          <div>
            <div className="font-mono text-xl font-bold text-severity-high leading-none">
              {count}
            </div>
            <div className="text-[11px] text-text-tertiary mt-0.5">
              potential CVEs detected
            </div>
          </div>
        </div>
        {expanded
          ? <ChevronUp className="h-4 w-4 text-text-tertiary" />
          : <ChevronDown className="h-4 w-4 text-text-tertiary" />
        }
      </button>

      {expanded && (
        <div className="space-y-1.5 max-h-[200px] overflow-y-auto">
          {isLoadingAlerts ? (
            <div className="py-3 text-center">
              <div className="inline-block h-4 w-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : alerts?.map((alert, i) => (
            <div
              key={`${alert.cve_id}-${alert.image_reference}-${i}`}
              className="flex items-start gap-2 px-2.5 py-2 bg-bg-tertiary rounded-lg text-xs"
            >
              <span className={cn(
                'px-1.5 py-0.5 rounded font-bold text-[9px] flex-shrink-0 mt-0.5',
                alert.severity === 'CRITICAL'
                  ? 'bg-severity-critical/15 text-severity-critical'
                  : 'bg-severity-high/15 text-severity-high'
              )}>
                {alert.severity}
              </span>
              <div className="min-w-0">
                <div className="font-mono text-text-primary font-medium truncate">{alert.cve_id}</div>
                <div className="text-text-tertiary truncate">{alert.package_name}</div>
                <div className="font-mono text-[10px] text-text-tertiary truncate mt-0.5">{alert.image_reference}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
