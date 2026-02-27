import { type HTMLAttributes, type TdHTMLAttributes, type ThHTMLAttributes } from 'react';
import { cn } from '../../utils';

function Table({ className, ref, ...props }: HTMLAttributes<HTMLTableElement> & { ref?: React.Ref<HTMLTableElement> }) {
  return (
    <div className="relative w-full overflow-auto">
      <table
        ref={ref}
        className={cn('w-full caption-bottom text-sm', className)}
        {...props}
      />
    </div>
  );
}

function TableHeader({ className, ref, ...props }: HTMLAttributes<HTMLTableSectionElement> & { ref?: React.Ref<HTMLTableSectionElement> }) {
  return <thead ref={ref} className={cn('[&_tr]:border-b', className)} {...props} />;
}

function TableBody({ className, ref, ...props }: HTMLAttributes<HTMLTableSectionElement> & { ref?: React.Ref<HTMLTableSectionElement> }) {
  return <tbody ref={ref} className={cn('[&_tr:last-child]:border-0', className)} {...props} />;
}

function TableFooter({ className, ref, ...props }: HTMLAttributes<HTMLTableSectionElement> & { ref?: React.Ref<HTMLTableSectionElement> }) {
  return (
    <tfoot
      ref={ref}
      className={cn('border-t border-border bg-bg-secondary/50 font-medium [&>tr]:last:border-b-0', className)}
      {...props}
    />
  );
}

function TableRow({ className, ref, ...props }: HTMLAttributes<HTMLTableRowElement> & { ref?: React.Ref<HTMLTableRowElement> }) {
  return (
    <tr
      ref={ref}
      className={cn(
        'border-b border-border transition-colors hover:bg-bg-tertiary/50 data-[state=selected]:bg-bg-tertiary',
        className
      )}
      {...props}
    />
  );
}

function TableHead({ className, ref, ...props }: ThHTMLAttributes<HTMLTableCellElement> & { ref?: React.Ref<HTMLTableCellElement> }) {
  return (
    <th
      ref={ref}
      className={cn(
        'h-10 px-4 text-left align-middle font-medium text-text-secondary [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]',
        className
      )}
      {...props}
    />
  );
}

function TableCell({ className, ref, ...props }: TdHTMLAttributes<HTMLTableCellElement> & { ref?: React.Ref<HTMLTableCellElement> }) {
  return (
    <td
      ref={ref}
      className={cn('p-4 align-middle [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]', className)}
      {...props}
    />
  );
}

function TableCaption({ className, ref, ...props }: HTMLAttributes<HTMLTableCaptionElement> & { ref?: React.Ref<HTMLTableCaptionElement> }) {
  return (
    <caption
      ref={ref}
      className={cn('mt-4 text-sm text-text-secondary', className)}
      {...props}
    />
  );
}

export { Table, TableHeader, TableBody, TableFooter, TableHead, TableRow, TableCell, TableCaption };
