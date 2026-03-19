interface StatusOverviewProps {
  statuses: { label: string; value: number; color: string }[];
}

export function StatusOverview({ statuses }: StatusOverviewProps) {
  return (
    <div className="flex flex-col gap-2">
      {statuses.map(s => (
        <div
          key={s.label}
          className="flex items-center gap-2.5 bg-bg-tertiary rounded-[10px] px-3.5 py-2.5 hover:bg-bg-hover transition-colors"
        >
          <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: s.color }} />
          <div className="flex-1">
            <div className="text-xs text-text-secondary">{s.label}</div>
            <div className="font-mono text-lg font-medium text-text-primary leading-none">
              {s.value.toLocaleString()}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
