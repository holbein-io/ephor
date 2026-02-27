import { type ComponentPropsWithoutRef } from 'react';
import * as TabsPrimitive from '@radix-ui/react-tabs';
import { cn } from '../../utils';

const Tabs = TabsPrimitive.Root;

function TabsList({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof TabsPrimitive.List> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <TabsPrimitive.List
      ref={ref}
      className={cn(
        'inline-flex h-9 items-center justify-center rounded-lg bg-bg-secondary p-1 text-text-secondary',
        className
      )}
      {...props}
    />
  );
}

function TabsTrigger({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger> & { ref?: React.Ref<HTMLButtonElement> }) {
  return (
    <TabsPrimitive.Trigger
      ref={ref}
      className={cn(
        'inline-flex items-center justify-center whitespace-nowrap rounded-md px-3 py-1 text-sm font-medium ring-offset-bg-primary transition-all',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-offset-2',
        'disabled:pointer-events-none disabled:opacity-50',
        'data-[state=active]:bg-bg-card data-[state=active]:text-text-primary data-[state=active]:shadow-sm',
        className
      )}
      {...props}
    />
  );
}

function TabsContent({
  className,
  ref,
  ...props
}: ComponentPropsWithoutRef<typeof TabsPrimitive.Content> & { ref?: React.Ref<HTMLDivElement> }) {
  return (
    <TabsPrimitive.Content
      ref={ref}
      className={cn(
        'mt-2 ring-offset-bg-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-offset-2',
        className
      )}
      {...props}
    />
  );
}

export { Tabs, TabsList, TabsTrigger, TabsContent };
