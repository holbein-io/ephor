interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const buttonClass =
    'px-3 py-1.5 rounded-lg text-xs font-medium bg-bg-tertiary text-text-secondary border border-border hover:border-accent disabled:opacity-40 disabled:cursor-not-allowed transition-colors';

  return (
    <div className="flex items-center justify-between px-5 py-3 border-t border-border">
      <span className="text-xs text-text-tertiary">Page {page + 1} of {totalPages}</span>
      <div className="flex gap-2">
        <button onClick={() => onPageChange(Math.max(0, page - 1))} disabled={page === 0} className={buttonClass}>
          Previous
        </button>
        <button onClick={() => onPageChange(page + 1)} disabled={page + 1 >= totalPages} className={buttonClass}>
          Next
        </button>
      </div>
    </div>
  );
}
