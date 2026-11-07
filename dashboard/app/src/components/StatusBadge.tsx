import { cn } from '../utils';
import { formatStatus, orderedStatusCounts, ACTIONABLE_STATUSES } from '../utils';
import { Tooltip, TooltipTrigger, TooltipContent } from './ui/tooltip';

interface StatusBadgeProps {
  status: string;
  /** Workloads per status; when given for a mixed status, the badge names what is still actionable. */
  counts?: Record<string, number>;
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

const baseClass = 'inline-flex items-center gap-[5px] px-2.5 py-0.5 rounded-full text-caption font-medium whitespace-nowrap';
const mutedStyle = 'bg-bg-tertiary text-text-secondary';

export function StatusBadge({ status, counts, className }: StatusBadgeProps) {
  const lower = status.toLowerCase();

  if (lower !== 'mixed' || !counts) {
    return (
      <span className={cn(baseClass, statusStyles[lower] || mutedStyle, className)}>
        {formatStatus(status)}
      </span>
    );
  }

  const entries = orderedStatusCounts(counts);
  const actionable = entries.find(([s]) => ACTIONABLE_STATUSES.has(s));

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span className={cn(baseClass, 'cursor-default', actionable ? statusStyles[actionable[0]] : mutedStyle, className)}>
          Mixed
          {actionable && <span>· {actionable[1]} {formatStatus(actionable[0]).toLowerCase()}</span>}
        </span>
      </TooltipTrigger>
      <TooltipContent>
        <div className="flex flex-col gap-0.5">
          {entries.map(([s, n]) => (
            <span key={s} className="flex justify-between gap-4">
              <span>{formatStatus(s)}</span>
              <span className="font-mono">{n}</span>
            </span>
          ))}
          <span className="text-text-tertiary pt-1">workloads per status</span>
        </div>
      </TooltipContent>
    </Tooltip>
  );
}
