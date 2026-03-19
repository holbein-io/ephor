export const CHART_COLORS = {
  grid: '#2A2A2E',
  text: '#8A8680',
  tooltipBg: '#141416',
  tooltipBorder: '#2A2A2E',
  axisLine: '#2A2A2E',
  cursor: '#333338',
};

export const SEVERITY_CHART_COLORS = {
  CRITICAL: '#E8613A',
  HIGH: '#E8A33A',
  MEDIUM: '#C9B84A',
  LOW: '#5B8DEF',
  UNKNOWN: '#6B7280',
};

export const STATUS_CHART_COLORS = {
  open: '#E8613A',
  resolved: '#3ECFA5',
  accepted_risk: '#E8A33A',
  false_positive: '#6B7280',
};

export const chartTooltipStyle = {
  contentStyle: {
    backgroundColor: CHART_COLORS.tooltipBg,
    border: `1px solid ${CHART_COLORS.tooltipBorder}`,
    borderRadius: '12px',
    color: '#F0EDE8',
    fontSize: '12px',
    fontFamily: 'DM Sans, system-ui, sans-serif',
  },
  labelStyle: {
    color: '#8A8680',
  },
};
