import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './ui/button';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({ currentPage, totalPages, onPageChange, disabled }: PaginationProps) {
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

  return (
    <div className="flex items-center justify-between border-t border-border bg-bg-card px-4 py-3 sm:px-6">
      <div className="flex flex-1 justify-between sm:hidden">
        <Button
          variant="outline"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={disabled || currentPage <= 1}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={disabled || currentPage >= totalPages}
        >
          Next
        </Button>
      </div>

      <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
        <div>
          <p className="text-sm text-text-secondary">
            Page <span className="font-medium">{currentPage}</span> of{' '}
            <span className="font-medium">{totalPages}</span>
          </p>
        </div>

        <div>
          <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(currentPage - 1)}
              disabled={disabled || currentPage <= 1}
              className="rounded-r-none"
            >
              <ChevronLeft className="h-4 w-4" />
              <span className="sr-only">Previous</span>
            </Button>

            {visiblePages.map((page, index) => (
              <div key={index}>
                {page === '...' ? (
                  <span className="relative inline-flex items-center px-4 py-2 text-sm font-medium text-text-secondary bg-bg-card border border-border">
                    ...
                  </span>
                ) : (
                  <Button
                    variant={page === currentPage ? 'primary' : 'outline'}
                    size="sm"
                    onClick={() => onPageChange(page as number)}
                    disabled={disabled}
                    className="rounded-none"
                  >
                    {page}
                  </Button>
                )}
              </div>
            ))}

            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(currentPage + 1)}
              disabled={disabled || currentPage >= totalPages}
              className="rounded-l-none"
            >
              <ChevronRight className="h-4 w-4" />
              <span className="sr-only">Next</span>
            </Button>
          </nav>
        </div>
      </div>
    </div>
  );
}