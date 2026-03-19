import { cn } from '../../utils';

interface BentoCardProps {
  title: string;
  action?: { label: string; href: string };
  span?: 3 | 4 | 5 | 6 | 7 | 8 | 12;
  className?: string;
  children: React.ReactNode;
}

const spanClasses: Record<number, string> = {
  3: 'col-span-3',
  4: 'col-span-4',
  5: 'col-span-5',
  6: 'col-span-6',
  7: 'col-span-7',
  8: 'col-span-8',
  12: 'col-span-12',
};

export function BentoCard({ title, action, span = 4, className, children }: BentoCardProps) {
  return (
    <div
      className={cn(
        'bg-bg-secondary border border-border rounded-2xl overflow-hidden transition-shadow duration-200 hover:shadow-[0_8px_32px_rgba(0,0,0,0.3)] animate-fade-up',
        spanClasses[span],
        className
      )}
    >
      <div className="flex items-center justify-between px-[22px] pt-[18px] mb-3.5">
        <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
          {title}
        </span>
        {action && (
          <a
            href={action.href}
            className="font-mono text-[11px] font-semibold text-accent hover:underline"
          >
            {action.label}
          </a>
        )}
      </div>
      <div className="px-[22px] pb-5">
        {children}
      </div>
    </div>
  );
}
