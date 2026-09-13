import { X } from 'lucide-react';

interface FilterPill<K extends string> {
  key: K;
  label: string;
  value: string;
}

interface FilterPillsProps<K extends string> {
  filters: FilterPill<K>[];
  onRemove: (key: K) => void;
  onClearAll: () => void;
}

export function FilterPills<K extends string>({ filters, onRemove, onClearAll }: FilterPillsProps<K>) {
  if (filters.length === 0) return null;

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-xs text-text-tertiary">Active:</span>

      {filters.map((filter) => (
        <span
          key={filter.key}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-accent-dim border border-accent/20 rounded-full text-xs text-accent"
        >
          {filter.label}: {filter.value}
          <button
            onClick={() => onRemove(filter.key)}
            className="opacity-70 hover:opacity-100 transition-opacity"
            aria-label={`Remove ${filter.label} filter`}
          >
            <X className="h-3 w-3" />
          </button>
        </span>
      ))}

      <button
        onClick={onClearAll}
        className="text-xs text-text-tertiary hover:text-text-secondary ml-auto cursor-pointer transition-colors"
      >
        Clear all
      </button>
    </div>
  );
}
