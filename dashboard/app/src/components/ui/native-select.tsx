import { type SelectHTMLAttributes } from 'react';
import { Label } from './label';
import { cn } from '../../utils';

interface NativeSelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: { value: string; label: string }[];
  ref?: React.Ref<HTMLSelectElement>;
}

function NativeSelect({ className, label, error, options, id, ref, ...props }: NativeSelectProps) {
  const selectId = id || label?.toLowerCase().replace(/\s+/g, '-');

  return (
    <div className="space-y-1.5">
      {label && <Label htmlFor={selectId}>{label}</Label>}
      <select
        id={selectId}
        ref={ref}
        className={cn(
          'flex h-9 w-full rounded-md border border-border bg-bg-secondary px-3 py-1 text-sm text-text-primary shadow-sm transition-colors',
          'focus:outline-none focus:ring-2 focus:ring-accent/50',
          'disabled:cursor-not-allowed disabled:opacity-50',
          error && 'border-danger focus:ring-danger/50',
          className
        )}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <p className="text-sm text-danger">{error}</p>}
    </div>
  );
}

export { NativeSelect };
