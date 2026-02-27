import { type InputHTMLAttributes } from 'react';
import { cn } from '../../utils';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  ref?: React.Ref<HTMLInputElement>;
}

function Input({ className, type, ref, ...props }: InputProps) {
  return (
    <input
      type={type}
      className={cn(
        'flex h-9 w-full rounded-md border border-border bg-bg-secondary px-3 py-1 text-sm text-text-primary shadow-sm transition-colors',
        'file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-text-primary',
        'placeholder:text-text-tertiary',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/50',
        'disabled:cursor-not-allowed disabled:opacity-50',
        className
      )}
      ref={ref}
      {...props}
    />
  );
}

export { Input };
