import { type HTMLAttributes } from 'react';
import { cn } from '../../utils';

function Card({ className, ref, ...props }: HTMLAttributes<HTMLDivElement> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <div
      ref={ref}
      className={cn('rounded-2xl border border-border bg-bg-card', className)}
      {...props}
    />
  );
}

function CardHeader({ className, ref, ...props }: HTMLAttributes<HTMLDivElement> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <div
      ref={ref}
      className={cn('flex flex-col space-y-1.5 px-6 py-4 border-b border-border', className)}
      {...props}
    />
  );
}

function CardTitle({ className, ref, ...props }: HTMLAttributes<HTMLDivElement> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <div
      ref={ref}
      className={cn('font-mono text-[10px] font-medium tracking-[0.12em] uppercase text-text-tertiary', className)}
      {...props}
    />
  );
}

function CardDescription({ className, ref, ...props }: HTMLAttributes<HTMLDivElement> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <div
      ref={ref}
      className={cn('text-sm text-text-secondary', className)}
      {...props}
    />
  );
}

function CardContent({ className, ref, ...props }: HTMLAttributes<HTMLDivElement> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <div
      ref={ref}
      className={cn('px-6 py-4', className)}
      {...props}
    />
  );
}

function CardFooter({ className, ref, ...props }: HTMLAttributes<HTMLDivElement> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <div
      ref={ref}
      className={cn('flex items-center px-6 py-4 border-t border-border', className)}
      {...props}
    />
  );
}

export { Card, CardHeader, CardFooter, CardTitle, CardDescription, CardContent };
