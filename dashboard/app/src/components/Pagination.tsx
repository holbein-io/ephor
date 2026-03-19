import { cn } from '../utils';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalItems?: number;
  pageSize?: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({ currentPage, totalPages, totalItems, pageSize, onPageChange, disabled }: PaginationProps) {
  if (totalPages <= 1) return null;

  const getVisiblePages = () => {
    const delta = 2;
    const range: number[] = [];
    const rangeWithDots: (number | string)[] = [];

    for (let i = Math.max(2, currentPage - delta); i <= Math.min(totalPages - 1, currentPage + delta); i++) {
      range.push(i);
    }

    if (currentPage - delta > 2) {
      rangeWithDots.push(1, '...');
    } else {
      rangeWithDots.push(1);
    }

    rangeWithDots.push(...range);

    if (currentPage + delta < totalPages - 1) {
      rangeWithDots.push('...', totalPages);
    } else if (totalPages > 1) {
      rangeWithDots.push(totalPages);
    }

    return rangeWithDots;
  };

  const visiblePages = getVisiblePages();

  const rangeStart = totalItems && pageSize ? (currentPage - 1) * pageSize + 1 : null;
  const rangeEnd = totalItems && pageSize ? Math.min(currentPage * pageSize, totalItems) : null;

  return (
    <div className="flex items-center justify-between px-5 py-3.5 border-t border-border-subtle bg-bg-tertiary rounded-b-2xl">
      <span className="text-[12.5px] text-text-tertiary font-mono">
        {rangeStart && rangeEnd && totalItems
          ? `${rangeStart}-${rangeEnd} of ${totalItems.toLocaleString()}`
          : `Page ${currentPage} of ${totalPages}`
        }
      </span>

      <div className="flex gap-1">
        {visiblePages.map((page, index) => (
          <button
            key={index}
            onClick={() => typeof page === 'number' ? onPageChange(page) : undefined}
            disabled={disabled || page === '...'}
            className={cn(
              'flex items-center justify-center min-w-[30px] h-[30px] px-2 rounded-lg font-mono text-xs transition-all duration-150',
              page === currentPage
                ? 'bg-accent text-white border border-accent'
                : page === '...'
                  ? 'text-text-tertiary cursor-default'
                  : 'text-text-secondary hover:bg-bg-hover hover:text-text-primary cursor-pointer border border-transparent'
            )}
          >
            {page}
          </button>
        ))}
      </div>
    </div>
  );
}
