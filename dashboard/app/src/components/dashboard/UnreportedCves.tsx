import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AlertTriangle, ShieldCheck } from 'lucide-react';
import { sbomService } from '../../services/api';
import { cn } from '../../utils';
import { ImageRef } from '../ImageRef';
import { ExploitabilityChips } from '../sbom/ExploitabilityChips';

const PREVIEW_LIMIT = 8;

export function UnreportedCves() {
  const { data: summary, isLoading: isLoadingSummary } = useQuery({
    queryKey: ['unreported-cves-summary'],
    queryFn: () => sbomService.getUnreportedCveSummary(),
    refetchInterval: 60000,
  });

  const { data: page, isLoading: isLoadingPage } = useQuery({
    queryKey: ['unreported-cves', 'preview'],
    queryFn: () => sbomService.getUnreportedCves({}, 0, PREVIEW_LIMIT),
    refetchInterval: 60000,
  });

  if (isLoadingSummary) {
    return (
      <div className="flex gap-5 animate-pulse">
        <div className="w-[150px] h-24 bg-bg-tertiary rounded-xl" />
        <div className="flex-1 grid grid-cols-2 gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-14 bg-bg-tertiary rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  if (!summary || summary.total === 0) {
    return (
      <div className="flex items-center gap-3 py-6 text-sm text-text-tertiary">
        <div className="w-9 h-9 rounded-lg bg-accent-mint/10 flex items-center justify-center">
          <ShieldCheck className="h-4 w-4 text-accent-mint" />
        </div>
        <div>
          <div className="text-text-secondary font-medium">No unreported CVEs</div>
          <div className="text-xs text-text-tertiary mt-0.5">
            Every known CVE matching an SBOM package on a running image was reported by the scanner
          </div>
        </div>
      </div>
    );
  }

  const { total, kev_listed: kev, images } = summary;
  const imagesLabel = `${images} ${images === 1 ? 'image' : 'images'}`;
  // KEV-listed is the headline whenever there is one; a zero is never the big number.
  const headline = kev > 0 ? kev : total;
  const caption = kev > 0 ? 'KEV-listed on running workloads' : 'on running workloads';
  const secondary = kev > 0 ? `${total.toLocaleString()} total across ${imagesLabel}` : `across ${imagesLabel}`;
  const remaining = total - (page?.content.length ?? 0);

  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-stretch">
      <div className="flex sm:flex-col items-center sm:items-start gap-3 sm:gap-2 sm:w-[150px] sm:flex-shrink-0 sm:border-r sm:border-border sm:pr-5">
        <div className={cn('w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0',
          kev > 0 ? 'bg-severity-critical/10' : 'bg-severity-high/10')}>
          <AlertTriangle className={cn('h-5 w-5', kev > 0 ? 'text-severity-critical' : 'text-severity-high')} />
        </div>
        <div>
          <div className={cn('font-mono text-3xl font-bold leading-none', kev > 0 ? 'text-severity-critical' : 'text-severity-high')}>
            {headline}
          </div>
          <div className="text-caption text-text-tertiary mt-1.5 leading-snug">{caption}</div>
          <div className="text-caption text-text-tertiary mt-1 leading-snug">{secondary}</div>
        </div>
      </div>

      <div className="flex-1 min-w-0">
        {isLoadingPage ? (
          <div className="flex items-center justify-center py-8">
            <div className="h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {page?.content.map((cve) => (
              <Link
                key={`${cve.cve_id}-${cve.image_reference}`}
                to={`/vulnerabilities/${cve.vulnerability_id}`}
                className="flex items-center gap-2.5 px-3 py-2 bg-bg-tertiary rounded-lg border border-transparent hover:border-border transition-colors no-underline"
              >
                <span className={cn(
                  'px-1.5 py-0.5 rounded font-bold text-micro flex-shrink-0 tracking-wide',
                  cve.severity === 'CRITICAL'
                    ? 'bg-severity-critical/15 text-severity-critical'
                    : 'bg-severity-high/15 text-severity-high'
                )}>
                  {cve.severity}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-2">
                    <span className="font-mono text-xs text-text-primary font-medium truncate">{cve.cve_id}</span>
                    <ExploitabilityChips kevListed={cve.kev_listed} epssScore={cve.epss_score} />
                  </div>
                  <div className="mt-0.5 flex items-baseline gap-2 min-w-0">
                    <ImageRef references={cve.image_reference} showTag={false} className="text-micro min-w-0" />
                    <span className="text-micro text-text-tertiary truncate">{cve.namespaces.join(', ')}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}

        {remaining > 0 && (
          <div className="mt-2.5 text-caption text-text-tertiary font-mono">
            +{remaining} more
          </div>
        )}
      </div>
    </div>
  );
}
