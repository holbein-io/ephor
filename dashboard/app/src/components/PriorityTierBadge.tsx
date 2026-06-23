import { cn } from '../utils';
import { getPriorityTierColor } from '../constants/colors';

interface PriorityTierBadgeProps {
  tier?: string;
  className?: string;
}

// P3 (Monitor) is the noise floor; only render actionable tiers.
export function PriorityTierBadge({ tier, className }: PriorityTierBadgeProps) {
  if (!tier || tier === 'P3') return null;
  const style = getPriorityTierColor(tier);
  if (!style) return null;

  return (
    <span
      title={style.title}
      className={cn('inline-flex items-center rounded border font-semibold', style.tailwind, className)}
    >
      {style.label}
    </span>
  );
}
