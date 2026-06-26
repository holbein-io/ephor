import { useState } from 'react';
import { cn } from '../utils';
import { PriorityHeatmap } from './PriorityHeatmap';
import { SeverityHeatmap } from './SeverityHeatmap';

interface NamespaceHeatmapProps {
  maxNamespaces?: number;
}

const MODES = [
  { key: 'priority', label: 'Priority' },
  { key: 'severity', label: 'Severity' },
] as const;

type Mode = typeof MODES[number]['key'];

export function NamespaceHeatmap({ maxNamespaces = 6 }: NamespaceHeatmapProps) {
  const [mode, setMode] = useState<Mode>('priority');

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-0.5 bg-bg-tertiary rounded-lg p-0.5 w-fit">
        {MODES.map(m => (
          <button
            key={m.key}
            onClick={() => setMode(m.key)}
            className={cn(
              'px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors',
              mode === m.key
                ? 'bg-bg-card text-accent shadow-sm'
                : 'text-text-tertiary hover:text-text-secondary'
            )}
          >
            {m.label}
          </button>
        ))}
      </div>

      {mode === 'priority'
        ? <PriorityHeatmap maxNamespaces={maxNamespaces} />
        : <SeverityHeatmap maxNamespaces={maxNamespaces} />}
    </div>
  );
}
