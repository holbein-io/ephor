import { cn } from '../utils';
import { formatStatus } from '../utils';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

const statusStyles: Record<string, string> = {
  open: 'bg-severity-critical/10 text-severity-critical',
  in_progress: 'bg-accent-cool/10 text-accent-cool',
  triaged: 'bg-accent-mint/10 text-accent-mint',
  resolved: 'bg-accent-mint/10 text-accent-mint opacity-80',
  accepted_risk: 'bg-warning/10 text-warning',
  false_positive: 'bg-bg-tertiary text-text-secondary',
  planned: 'bg-accent-cool/10 text-accent-cool',
  completed: 'bg-accent-mint/10 text-accent-mint',
  abandoned: 'bg-bg-tertiary text-text-secondary',
};

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const lower = status.toLowerCase();
  const style = statusStyles[lower] || 'bg-bg-tertiary text-text-secondary';

  return (
    <span
      className={cn(
        'inline-flex items-center gap-[5px] px-2.5 py-0.5 rounded-full text-[11.5px] font-medium whitespace-nowrap',
        style,
        className
      )}
    >
      {formatStatus(status)}
    </span>
  );
}
