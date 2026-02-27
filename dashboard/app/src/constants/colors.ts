// Centralized color constants for the Holbein dark theme

export const SEVERITY_COLORS = {
  CRITICAL: {
    hex: '#EF4444',
    tailwind: 'text-severity-critical bg-severity-critical/15 border-severity-critical/30',
    textOnly: 'text-severity-critical',
    bgOnly: 'bg-severity-critical/15',
    borderOnly: 'border-severity-critical/30'
  },
  HIGH: {
    hex: '#F59E0B',
    tailwind: 'text-severity-high bg-severity-high/15 border-severity-high/30',
    textOnly: 'text-severity-high',
    bgOnly: 'bg-severity-high/15',
    borderOnly: 'border-severity-high/30'
  },
  MEDIUM: {
    hex: '#EAB308',
    tailwind: 'text-severity-medium bg-severity-medium/15 border-severity-medium/30',
    textOnly: 'text-severity-medium',
    bgOnly: 'bg-severity-medium/15',
    borderOnly: 'border-severity-medium/30'
  },
  LOW: {
    hex: '#3B82F6',
    tailwind: 'text-severity-low bg-severity-low/15 border-severity-low/30',
    textOnly: 'text-severity-low',
    bgOnly: 'bg-severity-low/15',
    borderOnly: 'border-severity-low/30'
  },
  UNKNOWN: {
    hex: '#6B7280',
    tailwind: 'text-severity-unknown bg-severity-unknown/15 border-severity-unknown/30',
    textOnly: 'text-severity-unknown',
    bgOnly: 'bg-severity-unknown/15',
    borderOnly: 'border-severity-unknown/30'
  }
} as const;

export const STATUS_COLORS = {
  accepted_risk: {
    tailwind: 'text-warning bg-warning/15 border-warning/30',
    label: 'Accepted Risk',
    textOnly: 'text-warning',
    bgOnly: 'bg-warning/15'
  },
  false_positive: {
    tailwind: 'text-text-secondary bg-bg-tertiary border-border',
    label: 'False Positive',
    textOnly: 'text-text-secondary',
    bgOnly: 'bg-bg-tertiary'
  },
  needs_remediation: {
    tailwind: 'text-danger bg-danger/15 border-danger/30',
    label: 'Needs Remediation',
    textOnly: 'text-danger',
    bgOnly: 'bg-danger/15'
  },
  duplicate: {
    tailwind: 'text-severity-low bg-severity-low/15 border-severity-low/30',
    label: 'Duplicate',
    textOnly: 'text-severity-low',
    bgOnly: 'bg-severity-low/15'
  },
  open: {
    tailwind: 'text-danger bg-danger/15 border-danger/30',
    label: 'Open',
    textOnly: 'text-danger',
    bgOnly: 'bg-danger/15'
  },
  resolved: {
    tailwind: 'text-success bg-success/15 border-success/30',
    label: 'Resolved',
    textOnly: 'text-success',
    bgOnly: 'bg-success/15'
  }
} as const;

export const PRIORITY_COLORS = {
  critical: {
    tailwind: 'text-severity-critical bg-severity-critical/15 border-severity-critical/30',
    label: 'Critical',
    textOnly: 'text-severity-critical',
    bgOnly: 'bg-severity-critical/15'
  },
  high: {
    tailwind: 'text-severity-high bg-severity-high/15 border-severity-high/30',
    label: 'High',
    textOnly: 'text-severity-high',
    bgOnly: 'bg-severity-high/15'
  },
  medium: {
    tailwind: 'text-severity-medium bg-severity-medium/15 border-severity-medium/30',
    label: 'Medium',
    textOnly: 'text-severity-medium',
    bgOnly: 'bg-severity-medium/15'
  },
  low: {
    tailwind: 'text-severity-low bg-severity-low/15 border-severity-low/30',
    label: 'Low',
    textOnly: 'text-severity-low',
    bgOnly: 'bg-severity-low/15'
  }
} as const;

export const TRIAGE_SESSION_STATUS_COLORS = {
  PREPARING: {
    tailwind: 'text-warning bg-warning/15',
    label: 'Preparing'
  },
  ACTIVE: {
    tailwind: 'text-success bg-success/15',
    label: 'Active'
  },
  COMPLETED: {
    tailwind: 'text-text-secondary bg-bg-tertiary',
    label: 'Completed'
  },
  CANCELLED: {
    tailwind: 'text-danger bg-danger/15',
    label: 'Cancelled'
  }
} as const;

export const REMEDIATION_STATUS_COLORS = {
  planned: {
    tailwind: 'text-severity-low bg-severity-low/15',
    label: 'Planned'
  },
  in_progress: {
    tailwind: 'text-warning bg-warning/15',
    label: 'In Progress'
  },
  completed: {
    tailwind: 'text-success bg-success/15',
    label: 'Completed'
  },
  abandoned: {
    tailwind: 'text-text-secondary bg-bg-tertiary',
    label: 'Abandoned'
  }
} as const;

export function getSeverityColor(severity: string): typeof SEVERITY_COLORS[keyof typeof SEVERITY_COLORS] {
  const upperSeverity = severity.toUpperCase() as keyof typeof SEVERITY_COLORS;
  return SEVERITY_COLORS[upperSeverity] || SEVERITY_COLORS.UNKNOWN;
}

export function getStatusColor(status: string): typeof STATUS_COLORS[keyof typeof STATUS_COLORS] | null {
  const lowerStatus = status.toLowerCase() as keyof typeof STATUS_COLORS;
  return STATUS_COLORS[lowerStatus] || null;
}

export function getPriorityColor(priority: string): typeof PRIORITY_COLORS[keyof typeof PRIORITY_COLORS] | null {
  const lowerPriority = priority.toLowerCase() as keyof typeof PRIORITY_COLORS;
  return PRIORITY_COLORS[lowerPriority] || null;
}

export function getTriageSessionStatusColor(status: string): typeof TRIAGE_SESSION_STATUS_COLORS[keyof typeof TRIAGE_SESSION_STATUS_COLORS] | null {
  const upperStatus = status.toUpperCase() as keyof typeof TRIAGE_SESSION_STATUS_COLORS;
  return TRIAGE_SESSION_STATUS_COLORS[upperStatus] || null;
}

export function getRemediationStatusColor(status: string): typeof REMEDIATION_STATUS_COLORS[keyof typeof REMEDIATION_STATUS_COLORS] | null {
  const lowerStatus = status.toLowerCase() as keyof typeof REMEDIATION_STATUS_COLORS;
  return REMEDIATION_STATUS_COLORS[lowerStatus] || null;
}
