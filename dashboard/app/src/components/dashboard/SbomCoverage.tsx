import { useQuery } from '@tanstack/react-query';
import { sbomService } from '../../services/api';

export function SbomCoverage() {
  const { data: coverage, isLoading } = useQuery({
    queryKey: ['sbom-coverage'],
    queryFn: () => sbomService.getCoverage(),
    refetchInterval: 60000,
  });

  if (isLoading || !coverage) {
    return (
      <div className="flex flex-col gap-3 animate-pulse">
        <div className="h-10 bg-bg-tertiary rounded-lg" />
        <div className="h-3 bg-bg-tertiary rounded-full" />
        <div className="h-16 bg-bg-tertiary rounded-lg" />
      </div>
    );
  }

  const { total_images, images_with_sbom, format_breakdown } = coverage;
  const percentage = total_images > 0 ? Math.round((images_with_sbom / total_images) * 100) : 0;
  const hasData = images_with_sbom > 0;

  if (!hasData && total_images === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-4 text-center">
        <p className="text-sm text-text-tertiary">No images scanned yet</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between">
        <div>
          <span className="font-mono text-2xl font-bold text-text-primary">{images_with_sbom}</span>
          <span className="font-mono text-sm text-text-tertiary ml-1">/ {total_images}</span>
        </div>
        <span className="font-mono text-sm font-medium text-text-secondary">{percentage}%</span>
      </div>

      <div className="w-full h-2 rounded-full bg-white/[0.03] overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-700 ease-out"
          style={{
            width: `${percentage}%`,
            background: percentage === 100
              ? 'var(--color-accent-mint)'
              : percentage >= 50
                ? 'var(--color-accent)'
                : 'var(--color-severity-medium)',
          }}
        />
      </div>

      {Object.keys(format_breakdown).length > 0 && (
        <div className="flex flex-col gap-1.5 mt-1">
          {Object.entries(format_breakdown).map(([format, count]) => (
            <div key={format} className="flex items-center justify-between text-xs">
              <span className="font-mono text-text-secondary uppercase tracking-wide">{format}</span>
              <span className="font-mono text-text-primary font-medium">{count}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
