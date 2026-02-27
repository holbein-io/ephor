import { X } from 'lucide-react';
import { VulnerabilityFilters } from '../types';

interface FilterPill {
  key: keyof VulnerabilityFilters;
  label: string;
  value: string;
}

interface FilterPillsProps {
  filters: FilterPill[];
  onRemove: (key: keyof VulnerabilityFilters) => void;
  onClearAll: () => void;
}

export function FilterPills({ filters, onRemove, onClearAll }: FilterPillsProps) {
  if (filters.length === 0) return null;

  return (
    <div className="flex flex-wrap items-center gap-2 py-3 px-4 bg-accent/10 border border-accent/20 rounded-lg">
      <span className="text-sm font-medium text-accent">Active filters:</span>

      {filters.map((filter) => (
        <span
          key={filter.key}
          className="inline-flex items-center gap-1.5 px-3 py-1 bg-bg-card border border-accent/30 rounded-full text-sm text-accent-hover shadow-sm"
        >
          <span className="font-medium">{filter.label}:</span>
          <span className="text-accent max-w-[150px] truncate" title={filter.value}>
            {filter.value}
          </span>
          <button
            onClick={() => onRemove(filter.key)}
            className="ml-1 p-0.5 rounded-full hover:bg-accent/15 transition-colors"
            aria-label={`Remove ${filter.label} filter`}
          >
            <X className="h-3 w-3 text-accent" />
          </button>
        </span>
      ))}

      <button
        onClick={onClearAll}
        className="ml-2 text-sm text-accent hover:text-accent-hover font-medium underline-offset-2 hover:underline transition-colors"
      >
        Clear all
      </button>
    </div>
  );
}
