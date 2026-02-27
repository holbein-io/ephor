import { type HTMLAttributes } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors',
  {
    variants: {
      variant: {
        default: 'border-border bg-bg-tertiary text-text-primary',
        secondary: 'border-transparent bg-bg-tertiary text-text-secondary',
        destructive: 'border-danger/30 bg-danger/15 text-danger',
        outline: 'border-border text-text-primary',
        critical: 'border-severity-critical/30 bg-severity-critical/15 text-severity-critical',
        high: 'border-severity-high/30 bg-severity-high/15 text-severity-high',
        medium: 'border-severity-medium/30 bg-severity-medium/15 text-severity-medium',
        low: 'border-severity-low/30 bg-severity-low/15 text-severity-low',
        unknown: 'border-severity-unknown/30 bg-severity-unknown/15 text-severity-unknown',
        success: 'border-success/30 bg-success/15 text-success',
        warning: 'border-warning/30 bg-warning/15 text-warning',
        danger: 'border-danger/30 bg-danger/15 text-danger',
        info: 'border-severity-low/30 bg-severity-low/15 text-severity-low',
        error: 'border-danger/30 bg-danger/15 text-danger',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

interface BadgeProps
  extends HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
