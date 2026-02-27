import { type ComponentPropsWithoutRef } from 'react';
import * as LabelPrimitive from '@radix-ui/react-label';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../utils';

const labelVariants = cva(
  'text-sm font-medium leading-none text-text-secondary peer-disabled:cursor-not-allowed peer-disabled:opacity-70'
);

function Label({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof LabelPrimitive.Root> &
  VariantProps<typeof labelVariants> & { ref?: React.Ref<HTMLLabelElement> }) {
  return (
    <LabelPrimitive.Root
      ref={ref}
      className={cn(labelVariants(), className)}
      {...props}
    />
  );
}

export { Label };
