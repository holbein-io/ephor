import { useQuery } from '@tanstack/react-query';
import { ThreatStrip } from '../components/dashboard/ThreatStrip';
import { SeverityMegaStrip } from '../components/dashboard/SeverityMegaStrip';
import { BentoCard } from '../components/dashboard/BentoCard';
import { StatusOverview } from '../components/dashboard/StatusOverview';
import { NamespaceHeatmap } from '../components/NamespaceHeatmap';
import { ActivityFeed } from '../components/ActivityFeed';
import { SbomCoverage } from '../components/dashboard/SbomCoverage';
import { PreScanAlerts } from '../components/dashboard/PreScanAlerts';
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
    { label: 'In Progress', value: metrics.by_status.triaged, color: 'var(--color-accent-cool)' },
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
            total={metrics.total_active_vulnerabilities}
          />

          <BentoCard title="Status Overview" span={3}>
            <StatusOverview statuses={statusItems} />
          </BentoCard>

          <BentoCard
            title="Namespace Heatmap"
            action={{ label: 'Expand →', href: '/vulnerabilities' }}
            span={5}
          >
            <NamespaceHeatmap maxNamespaces={5} />
          </BentoCard>

          {hasPermission('VIEW_ADMIN') && (
            <BentoCard
              title="Recent Activity"
              action={{ label: 'All →', href: '#' }}
              span={4}
            >
              <ActivityFeed limit={8} />
            </BentoCard>
          )}

          <BentoCard title="SBOM Coverage" span={4}>
            <SbomCoverage />
          </BentoCard>

          <BentoCard
            title="Pre-Scan Alerts"
            action={{ label: 'View all →', href: '/inventory?tab=prescan' }}
            span={hasPermission('VIEW_ADMIN') ? 8 : 12}
          >
            <PreScanAlerts />
          </BentoCard>
        </div>
      </div>
    </div>
  );
}
