import { cn } from '../utils';

interface SeverityBadgeProps {
  severity: string;
  className?: string;
}

const severityStyles: Record<string, { dot: string; badge: string }> = {
  CRITICAL: {
    dot: 'bg-severity-critical',
    badge: 'bg-severity-critical/10 text-severity-critical border-severity-critical/20',
  },
  HIGH: {
    dot: 'bg-severity-high',
    badge: 'bg-severity-high/10 text-severity-high border-severity-high/20',
  },
  MEDIUM: {
    dot: 'bg-severity-medium',
    badge: 'bg-severity-medium/10 text-severity-medium border-severity-medium/20',
  },
  LOW: {
    dot: 'bg-severity-low',
    badge: 'bg-severity-low/10 text-severity-low border-severity-low/20',
  },
  UNKNOWN: {
    dot: 'bg-severity-unknown',
    badge: 'bg-severity-unknown/10 text-severity-unknown border-severity-unknown/20',
  },
};

export function SeverityBadge({ severity, className }: SeverityBadgeProps) {
  const upper = severity.toUpperCase();
  const style = severityStyles[upper] || severityStyles.UNKNOWN;

  return (
    <span
      className={cn(
        'inline-flex items-center gap-[5px] px-2.5 py-0.5 rounded-full text-[11.5px] font-semibold border whitespace-nowrap',
        style.badge,
        className
      )}
    >
      <span className={cn('w-1.5 h-1.5 rounded-full', style.dot)} />
      {severity}
    </span>
  );
}
