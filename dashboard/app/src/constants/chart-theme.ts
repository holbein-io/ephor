// Recharts dark theme constants for the Holbein design system

export const CHART_COLORS = {
  grid: '#23232A',
  text: '#9A9690',
  tooltipBg: '#111115',
  tooltipBorder: '#23232A',
  axisLine: '#23232A',
  cursor: '#2E2E36',
};

export const SEVERITY_CHART_COLORS = {
  CRITICAL: '#EF4444',
  HIGH: '#F59E0B',
  MEDIUM: '#EAB308',
  LOW: '#3B82F6',
  UNKNOWN: '#6B7280',
};

export const STATUS_CHART_COLORS = {
  open: '#B83B3B',
  resolved: '#4A9E6A',
  accepted_risk: '#C4973B',
  false_positive: '#6B7280',
};

export const chartTooltipStyle = {
  contentStyle: {
    backgroundColor: CHART_COLORS.tooltipBg,
    border: `1px solid ${CHART_COLORS.tooltipBorder}`,
    borderRadius: '8px',
    color: '#E8E4DE',
    fontSize: '12px',
  },
  labelStyle: {
    color: '#9A9690',
  },
};
