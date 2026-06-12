import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ShieldCheck } from 'lucide-react';
import { sbomService } from '../../services/api';
import { cn } from '../../utils';

const ALERT_LIMIT = 200;

export function PreScanAlertsList() {
  const { data: alerts, isLoading } = useQuery({
    queryKey: ['pre-scan-alerts-full'],
    queryFn: () => sbomService.getPreScanAlerts(ALERT_LIMIT),
    refetchInterval: 60000,
  });

  if (isLoading) {
    return (
      <div className="px-6 py-12 text-center">
        <div className="inline-block h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!alerts?.length) {
    return (
      <div className="px-6 py-12 text-center">
        <ShieldCheck className="h-8 w-8 text-accent-mint mx-auto mb-3" />
        <p className="text-sm text-text-tertiary">
          No pre-scan alerts. No known CVEs match SBOM packages on images that haven't been scanned yet.
        </p>
      </div>
    );
  }

  const critical = alerts.filter(a => a.severity === 'CRITICAL').length;
  const summary =
    `${alerts.length.toLocaleString()} known ${critical > 0 ? `(${critical} critical) ` : ''}` +
    'CVEs match packages on images not yet scanned' +
    (alerts.length === ALERT_LIMIT ? ` — showing first ${ALERT_LIMIT}` : '');

  return (
    <>
      <p className="text-[13px] text-text-secondary -mt-1">{summary}</p>

      <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border">
              <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Severity</th>
              <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">CVE</th>
              <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Package</th>
              <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Version</th>
              <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Image</th>
            </tr>
          </thead>
          <tbody>
            {alerts.map((alert, i) => (
              <tr
                key={`${alert.cve_id}-${alert.image_reference}-${i}`}
                className="border-b border-border/50 last:border-b-0 hover:bg-bg-hover transition-colors"
              >
                <td className="px-5 py-3.5">
                  <span className={cn(
                    'inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold tracking-wide',
                    alert.severity === 'CRITICAL'
                      ? 'bg-severity-critical/15 text-severity-critical'
                      : 'bg-severity-high/15 text-severity-high'
                  )}>
                    <AlertTriangle className="h-3 w-3" />
                    {alert.severity}
                  </span>
                </td>
                <td className="px-5 py-3.5">
                  <span className="font-mono text-[13px] font-medium text-text-primary">{alert.cve_id}</span>
                  {alert.title && (
                    <span className="block text-[11px] text-text-tertiary truncate max-w-[280px] mt-0.5">{alert.title}</span>
                  )}
                </td>
                <td className="px-5 py-3.5">
                  <span className="font-mono text-[12px] text-text-secondary">{alert.package_name}</span>
                </td>
                <td className="px-5 py-3.5">
                  <span className="font-mono text-[12px] text-text-secondary">{alert.package_version}</span>
                </td>
                <td className="px-5 py-3.5">
                  <span className="font-mono text-[11px] text-text-tertiary truncate block max-w-[260px]">{alert.image_reference}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
