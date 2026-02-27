import { type InputHTMLAttributes } from 'react';
import { Label } from './label';
import { Input } from './input';
import { cn } from '../../utils';

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  ref?: React.Ref<HTMLInputElement>;
}

function FormField({ label, error, className, id, ref, ...props }: FormFieldProps) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');

  return (
    <div className="space-y-1.5">
      {label && <Label htmlFor={inputId}>{label}</Label>}
      <Input
        id={inputId}
        ref={ref}
        className={cn(error && 'border-danger focus-visible:ring-danger/50', className)}
        {...props}
      />
      {error && <p className="text-sm text-danger">{error}</p>}
    </div>
  );
}

export { FormField };
