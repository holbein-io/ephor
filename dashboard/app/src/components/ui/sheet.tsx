import { type ComponentPropsWithoutRef, type HTMLAttributes } from 'react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import { cva, type VariantProps } from 'class-variance-authority';
import { X } from 'lucide-react';
import { cn } from '../../utils';

const Sheet = SheetPrimitive.Root;
const SheetTrigger = SheetPrimitive.Trigger;
const SheetClose = SheetPrimitive.Close;
const SheetPortal = SheetPrimitive.Portal;

function SheetOverlay({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof SheetPrimitive.Overlay> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <SheetPrimitive.Overlay
      className={cn(
        'fixed inset-0 z-50 bg-black/80',
        'data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
        className
      )}
      ref={ref}
      {...props}
    />
  );
}

const sheetVariants = cva(
  'fixed z-50 gap-4 bg-bg-card border-border p-6 shadow-lg transition ease-in-out data-[state=closed]:duration-300 data-[state=open]:duration-500 data-[state=open]:animate-in data-[state=closed]:animate-out',
  {
    variants: {
      side: {
        top: 'inset-x-0 top-0 border-b data-[state=closed]:slide-out-to-top data-[state=open]:slide-in-from-top',
        bottom: 'inset-x-0 bottom-0 border-t data-[state=closed]:slide-out-to-bottom data-[state=open]:slide-in-from-bottom',
        left: 'inset-y-0 left-0 h-full w-3/4 border-r data-[state=closed]:slide-out-to-left data-[state=open]:slide-in-from-left sm:max-w-sm',
        right: 'inset-y-0 right-0 h-full w-3/4 border-l data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right sm:max-w-sm',
      },
    },
    defaultVariants: {
      side: 'right',
    },
  }
);

interface SheetContentProps
  extends ComponentPropsWithoutRef<typeof SheetPrimitive.Content>,
    VariantProps<typeof sheetVariants> {
  ref?: React.Ref<HTMLDivElement>;
}

function SheetContent({ side = 'right', className, children, ref, ...props }: SheetContentProps) {
  return (
    <SheetPortal>
      <SheetOverlay />
      <SheetPrimitive.Content
        ref={ref}
        className={cn(sheetVariants({ side }), className)}
        {...props}
      >
        <SheetPrimitive.Close className="absolute right-4 top-4 rounded-sm opacity-70 ring-offset-bg-primary transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-accent/50 focus:ring-offset-2 disabled:pointer-events-none data-[state=open]:bg-bg-tertiary">
          <X className="h-4 w-4 text-text-secondary" />
          <span className="sr-only">Close</span>
        </SheetPrimitive.Close>
        {children}
      </SheetPrimitive.Content>
    </SheetPortal>
  );
}

function SheetHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('flex flex-col space-y-2 text-center sm:text-left', className)} {...props} />;
}

function SheetFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2', className)} {...props} />;
}

function SheetTitle({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof SheetPrimitive.Title> & { ref?: React.Ref<HTMLHeadingElement> }) {
  return (
    <SheetPrimitive.Title ref={ref} className={cn('text-lg font-semibold text-text-primary', className)} {...props} />
  );
}

function SheetDescription({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof SheetPrimitive.Description> & { ref?: React.Ref<HTMLParagraphElement> }) {
  return (
    <SheetPrimitive.Description ref={ref} className={cn('text-sm text-text-secondary', className)} {...props} />
  );
}

export {
  Sheet,
  SheetPortal,
  SheetOverlay,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
};
