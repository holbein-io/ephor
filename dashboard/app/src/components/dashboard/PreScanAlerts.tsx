import { useQuery } from '@tanstack/react-query';
import { AlertTriangle } from 'lucide-react';
import { sbomService } from '../../services/api';
import { cn } from '../../utils';

const PREVIEW_LIMIT = 8;

export function PreScanAlerts() {
  const { data: alertCount, isLoading: isLoadingCount } = useQuery({
    queryKey: ['pre-scan-alert-count'],
    queryFn: () => sbomService.getPreScanAlertCount(),
    refetchInterval: 60000,
  });

  const { data: alerts, isLoading: isLoadingAlerts } = useQuery({
    queryKey: ['pre-scan-alerts'],
    queryFn: () => sbomService.getPreScanAlerts(PREVIEW_LIMIT),
    refetchInterval: 60000,
  });

  if (isLoadingCount) {
    return (
      <div className="flex gap-5 animate-pulse">
        <div className="w-[150px] h-24 bg-bg-tertiary rounded-xl" />
        <div className="flex-1 grid grid-cols-2 gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-14 bg-bg-tertiary rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  const count = alertCount?.count ?? 0;

  if (count === 0) {
    return (
      <div className="flex items-center gap-3 py-6 text-sm text-text-tertiary">
        <div className="w-9 h-9 rounded-lg bg-accent-mint/10 flex items-center justify-center">
          <AlertTriangle className="h-4 w-4 text-accent-mint" />
        </div>
        <div>
          <div className="text-text-secondary font-medium">No pre-scan alerts</div>
          <div className="text-xs text-text-tertiary mt-0.5">
            No known CVEs detected in SBOM packages ahead of a full scan
          </div>
        </div>
      </div>
    );
  }

  const remaining = count - (alerts?.length ?? 0);

  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-stretch">
      {/* Headline count */}
      <div className="flex sm:flex-col items-center sm:items-start gap-3 sm:gap-2 sm:w-[150px] sm:flex-shrink-0 sm:border-r sm:border-border sm:pr-5">
        <div className="w-11 h-11 rounded-xl bg-severity-high/10 flex items-center justify-center flex-shrink-0">
          <AlertTriangle className="h-5 w-5 text-severity-high" />
        </div>
        <div>
          <div className="font-mono text-3xl font-bold text-severity-high leading-none">
            {count}
          </div>
          <div className="text-[11px] text-text-tertiary mt-1.5 leading-snug">
            potential CVEs detected before scan
          </div>
        </div>
      </div>

      {/* Alert preview list */}
      <div className="flex-1 min-w-0">
        {isLoadingAlerts ? (
          <div className="flex items-center justify-center py-8">
            <div className="h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {alerts?.map((alert, i) => (
              <div
                key={`${alert.cve_id}-${alert.image_reference}-${i}`}
                className="flex items-center gap-2.5 px-3 py-2 bg-bg-tertiary rounded-lg border border-transparent hover:border-border transition-colors"
              >
                <span className={cn(
                  'px-1.5 py-0.5 rounded font-bold text-[9px] flex-shrink-0 tracking-wide',
                  alert.severity === 'CRITICAL'
                    ? 'bg-severity-critical/15 text-severity-critical'
                    : 'bg-severity-high/15 text-severity-high'
                )}>
                  {alert.severity}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-2">
                    <span className="font-mono text-xs text-text-primary font-medium truncate">
                      {alert.cve_id}
                    </span>
                    <span className="text-[11px] text-text-secondary truncate flex-shrink-0">
                      {alert.package_name}
                    </span>
                  </div>
                  <div className="font-mono text-[10px] text-text-tertiary truncate mt-0.5">
                    {alert.image_reference}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {remaining > 0 && (
          <div className="mt-2.5 text-[11px] text-text-tertiary font-mono">
            +{remaining} more {remaining === 1 ? 'alert' : 'alerts'}
          </div>
        )}
      </div>
    </div>
  );
}
