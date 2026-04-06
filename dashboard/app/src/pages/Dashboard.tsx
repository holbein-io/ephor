import { useQuery } from '@tanstack/react-query';
import { ThreatStrip } from '../components/dashboard/ThreatStrip';
import { SeverityMegaStrip } from '../components/dashboard/SeverityMegaStrip';
import { BentoCard } from '../components/dashboard/BentoCard';
import { StatusOverview } from '../components/dashboard/StatusOverview';
import { SeverityHeatmap } from '../components/SeverityHeatmap';
import { ActivityFeed } from '../components/ActivityFeed';
import { SbomCoverage } from '../components/dashboard/SbomCoverage';
import { dashboardService } from '../services/api';
import { useUser } from '../contexts/UserContext';

export function Dashboard() {
  const { hasPermission } = useUser();
  const { data: metrics, isLoading } = useQuery({
    queryKey: ['dashboard-metrics'],
    queryFn: () => dashboardService.getMetrics(),
    refetchInterval: 30000
  });

  if (isLoading || !metrics) {
    return (
      <div className="space-y-4 animate-pulse">
        <div className="h-[120px] bg-bg-secondary rounded-2xl" />
        <div className="h-16 bg-bg-secondary rounded-2xl" />
        <div className="grid grid-cols-12 gap-4">
          <div className="col-span-3 h-64 bg-bg-secondary rounded-2xl" />
          <div className="col-span-5 h-64 bg-bg-secondary rounded-2xl" />
          <div className="col-span-4 h-64 bg-bg-secondary rounded-2xl" />
        </div>
      </div>
    );
  }

  const statusItems = [
    { label: 'Open', value: metrics.by_status.open, color: 'var(--color-severity-critical)' },
    { label: 'In Progress', value: metrics.active_escalations, color: 'var(--color-accent-cool)' },
    { label: 'Resolved', value: metrics.by_status.resolved, color: 'var(--color-accent-mint)' },
    { label: 'Accepted', value: metrics.by_status.accepted_risk, color: 'var(--color-text-tertiary)' },
  ];

  return (
    <div className="-mx-6 -mt-7">
      <ThreatStrip metrics={metrics} />

      <div className="px-8 py-7 max-w-[1440px] mx-auto space-y-4">
        <div className="grid grid-cols-12 gap-4">
          <SeverityMegaStrip
            severity={metrics.by_severity}
            total={metrics.total_vulnerabilities}
          />

          <BentoCard title="Status Overview" span={3}>
            <StatusOverview statuses={statusItems} />
          </BentoCard>

          <BentoCard
            title="Namespace Heatmap"
            action={{ label: 'Expand →', href: '/vulnerabilities' }}
            span={5}
          >
            <SeverityHeatmap maxNamespaces={5} />
          </BentoCard>

          {hasPermission('VIEW_ADMIN') ? (
            <BentoCard
              title="Recent Activity"
              action={{ label: 'All →', href: '#' }}
              span={4}
            >
              <ActivityFeed limit={8} />
            </BentoCard>
          ) : (
            <BentoCard title="Summary" span={4}>
              <div className="flex flex-col gap-3 text-sm text-text-secondary">
                <div className="flex justify-between">
                  <span>Total vulnerabilities</span>
                  <span className="font-mono font-medium text-text-primary">
                    {metrics.total_vulnerabilities.toLocaleString()}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>False positives</span>
                  <span className="font-mono font-medium text-text-primary">
                    {metrics.by_status.false_positive}
                  </span>
                </div>
              </div>
            </BentoCard>
          )}

          <BentoCard title="SBOM Coverage" span={3}>
            <SbomCoverage />
          </BentoCard>
        </div>
      </div>
    </div>
  );
}
