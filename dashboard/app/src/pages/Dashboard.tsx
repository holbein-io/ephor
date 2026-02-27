import { useQuery } from '@tanstack/react-query';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { SeverityHeatmap } from '../components/SeverityHeatmap';
import { dashboardService } from '../services/api';
import { CHART_COLORS, SEVERITY_CHART_COLORS, chartTooltipStyle } from '../constants/chart-theme';

export function Dashboard() {
  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: ['dashboard-metrics'],
    queryFn: () => dashboardService.getMetrics(),
    refetchInterval: 30000
  });

  const { data: trends, isLoading: trendsLoading } = useQuery({
    queryKey: ['vulnerability-trends'],
    queryFn: () => dashboardService.getTrends(30),
    refetchInterval: 60000
  });

  if (metricsLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {[...Array(2)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-40 bg-bg-tertiary rounded"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  const severityData = metrics ? [
    { name: 'Critical', value: metrics.by_severity.CRITICAL, color: SEVERITY_CHART_COLORS.CRITICAL },
    { name: 'High', value: metrics.by_severity.HIGH, color: SEVERITY_CHART_COLORS.HIGH },
    { name: 'Medium', value: metrics.by_severity.MEDIUM, color: SEVERITY_CHART_COLORS.MEDIUM },
    { name: 'Low', value: metrics.by_severity.LOW, color: SEVERITY_CHART_COLORS.LOW },
    { name: 'Unknown', value: metrics.by_severity.UNKNOWN, color: SEVERITY_CHART_COLORS.UNKNOWN }
  ].filter(item => item.value > 0) : [];

  return (
    <div className="space-y-6">
      {/* Row 1: Status Breakdown + Severity Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="font-display">Status Breakdown</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-danger rounded-full"></div>
                  <span className="text-sm font-medium text-text-secondary">Open</span>
                </div>
                <span className="text-lg font-bold text-danger">{metrics?.by_status.open || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-success rounded-full"></div>
                  <span className="text-sm font-medium text-text-secondary">Resolved</span>
                </div>
                <span className="text-lg font-bold text-success">{metrics?.by_status.resolved || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-warning rounded-full"></div>
                  <span className="text-sm font-medium text-text-secondary">Accepted Risk</span>
                </div>
                <span className="text-lg font-bold text-warning">{metrics?.by_status.accepted_risk || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 bg-severity-unknown rounded-full"></div>
                  <span className="text-sm font-medium text-text-secondary">False Positives</span>
                </div>
                <span className="text-lg font-bold text-text-secondary">{metrics?.by_status.false_positive || 0}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="font-display">Severity Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            {severityData.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={severityData}
                    cx="50%"
                    cy="50%"
                    outerRadius={80}
                    dataKey="value"
                    label={({ name, value }) => `${name}: ${value}`}
                  >
                    {severityData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip {...chartTooltipStyle} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-64 flex items-center justify-center text-text-tertiary">
                No vulnerability data available
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Row 2: Vulnerability Trends (full width) */}
      <Card>
        <CardHeader>
          <CardTitle className="font-display">Vulnerability Trends (30 days)</CardTitle>
        </CardHeader>
        <CardContent>
          {trendsLoading ? (
            <div className="h-64 animate-pulse bg-bg-tertiary rounded"></div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={trends}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} />
                <XAxis
                  dataKey="date"
                  tickFormatter={(value) => new Date(value).toLocaleDateString()}
                  tick={{ fill: CHART_COLORS.text }}
                  axisLine={{ stroke: CHART_COLORS.axisLine }}
                />
                <YAxis
                  tick={{ fill: CHART_COLORS.text }}
                  axisLine={{ stroke: CHART_COLORS.axisLine }}
                />
                <Tooltip
                  {...chartTooltipStyle}
                  labelFormatter={(value) => new Date(value as string).toLocaleDateString()}
                />
                <Bar dataKey="critical" stackId="a" fill={SEVERITY_CHART_COLORS.CRITICAL} />
                <Bar dataKey="high" stackId="a" fill={SEVERITY_CHART_COLORS.HIGH} />
                <Bar dataKey="medium" stackId="a" fill={SEVERITY_CHART_COLORS.MEDIUM} />
                <Bar dataKey="low" stackId="a" fill={SEVERITY_CHART_COLORS.LOW} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      {/* Row 3: Severity Heatmap by Namespace (full width) */}
      <Card>
        <CardHeader>
          <CardTitle className="font-display">Severity Heatmap by Namespace</CardTitle>
        </CardHeader>
        <CardContent>
          <SeverityHeatmap maxNamespaces={8} />
        </CardContent>
      </Card>
    </div>
  );
}
