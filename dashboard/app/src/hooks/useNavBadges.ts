import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/api';

export interface NavBadges {
  vulnerabilities: number;
  criticalHigh: number;
  escalations: number;
}

/**
 * Hook to fetch navigation badge counts
 * Used by sidebar to show counts next to nav items
 */
export function useNavBadges() {
  const { data: metrics } = useQuery({
    queryKey: ['nav-badges-metrics'],
    queryFn: () => dashboardService.getMetrics(),
    refetchInterval: 60000, // Refresh every minute
    staleTime: 30000
  });

  const badges: NavBadges = {
    vulnerabilities: metrics?.by_status?.open || 0,
    criticalHigh: (metrics?.by_severity?.CRITICAL || 0) + (metrics?.by_severity?.HIGH || 0),
    escalations: metrics?.active_escalations || 0
  };

  return badges;
}
