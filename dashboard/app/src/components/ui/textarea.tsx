import { type TextareaHTMLAttributes } from 'react';
import { cn } from '../../utils';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  ref?: React.Ref<HTMLTextAreaElement>;
}

function Textarea({ className, ref, ...props }: TextareaProps) {
  return (
    <textarea
      className={cn(
        'flex min-h-[60px] w-full rounded-md border border-border bg-bg-secondary px-3 py-2 text-sm text-text-primary shadow-sm',
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

export { Textarea };
