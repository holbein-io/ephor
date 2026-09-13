import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ShieldCheck, Flame } from 'lucide-react';
import { sbomService, dashboardService } from '../../services/api';
import { UnreportedCveFilters } from '../../types';
import { cn } from '../../utils';
import { ImageRef } from '../ImageRef';
import { SeverityBadge } from '../SeverityBadge';
import { FilterPills } from '../FilterPills';
import { Pagination } from '../Pagination';
import { NativeSelect } from '../ui/native-select';
import { ExploitabilityChips } from './ExploitabilityChips';

const PAGE_SIZE = 25;

const severityOptions = [
  { value: '', label: 'Critical + High' },
  { value: 'CRITICAL', label: 'Critical' },
  { value: 'HIGH', label: 'High' },
];

const minEpssOptions = [
  { value: '', label: 'Any EPSS' },
  { value: '0.1', label: 'EPSS ≥ 10%' },
  { value: '0.5', label: 'EPSS ≥ 50%' },
];

export function UnreportedCvesList() {
  const [filters, setFilters] = useState<UnreportedCveFilters>({});
  const [page, setPage] = useState(0);

  const updateFilters = (patch: UnreportedCveFilters) => {
    setFilters(prev => ({ ...prev, ...patch }));
    setPage(0);
  };

  const { data: namespaces } = useQuery({
    queryKey: ['namespaces'],
    queryFn: () => dashboardService.getNamespaces(),
    staleTime: 5 * 60 * 1000,
  });

  const { data: result, isLoading } = useQuery({
    queryKey: ['unreported-cves', filters, page],
    queryFn: () => sbomService.getUnreportedCves(filters, page, PAGE_SIZE),
    refetchInterval: 60000,
  });

  const namespaceOptions = [
    { value: '', label: 'All Namespaces' },
    ...(namespaces || []).map(ns => ({ value: ns, label: ns })),
  ];

  const activeFilters: Array<{ key: keyof UnreportedCveFilters; label: string; value: string }> = [];
  if (filters.namespace) {
    activeFilters.push({ key: 'namespace', label: 'Namespace', value: filters.namespace });
  }
  if (filters.severity) {
    activeFilters.push({ key: 'severity', label: 'Severity', value: filters.severity });
  }
  if (filters.min_epss != null) {
    activeFilters.push({ key: 'min_epss', label: 'EPSS', value: `≥ ${filters.min_epss * 100}%` });
  }
  if (filters.kev_only) {
    activeFilters.push({ key: 'kev_only', label: 'Exploitation', value: 'KEV listed' });
  }

  const total = result?.total_elements;

  return (
    <>
      <p className="text-sm text-text-secondary -mt-1">
        A known CVE matches a package version in the image's SBOM, but the scanner did not report it on any
        workload running that image. Verify the scanner's coverage for these images.
      </p>

      <div className="bg-bg-secondary border border-border rounded-2xl px-[18px] py-3 flex flex-wrap items-center gap-3">
        <NativeSelect
          options={namespaceOptions}
          value={filters.namespace || ''}
          onChange={(e) => updateFilters({ namespace: e.target.value || undefined })}
          className="!h-8 !text-xs !w-[160px]"
        />
        <NativeSelect
          options={severityOptions}
          value={filters.severity || ''}
          onChange={(e) => updateFilters({ severity: e.target.value || undefined })}
          className="!h-8 !text-xs !w-[140px]"
        />
        <NativeSelect
          options={minEpssOptions}
          value={filters.min_epss != null ? String(filters.min_epss) : ''}
          onChange={(e) => updateFilters({ min_epss: e.target.value ? parseFloat(e.target.value) : undefined })}
          className="!h-8 !text-xs !w-[130px]"
        />
        <button
          onClick={() => updateFilters({ kev_only: filters.kev_only ? undefined : true })}
          className={cn(
            'inline-flex items-center gap-1 h-8 px-3 rounded-md text-xs font-medium border transition-colors',
            filters.kev_only
              ? 'text-danger bg-danger/10 border-danger/30'
              : 'text-text-secondary bg-bg-secondary border-border hover:border-accent'
          )}
        >
          <Flame className="h-3 w-3" />
          KEV only
        </button>
        <div className="flex-1" />
        {total !== undefined && (
          <span className="text-xs text-text-tertiary">
            <span className="font-medium text-text-secondary">{total.toLocaleString()}</span> unreported
          </span>
        )}
      </div>

      {activeFilters.length > 0 && (
        <FilterPills
          filters={activeFilters}
          onRemove={(key) => updateFilters({ [key]: undefined })}
          onClearAll={() => { setFilters({}); setPage(0); }}
        />
      )}

      <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden">
        {isLoading ? (
          <div className="px-6 py-12 text-center">
            <div className="inline-block h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : !result?.content.length ? (
          <div className="px-6 py-12 text-center">
            <ShieldCheck className="h-8 w-8 text-accent-mint mx-auto mb-3" />
            <p className="text-sm text-text-tertiary">
              {activeFilters.length > 0
                ? 'No unreported CVEs match these filters.'
                : 'No unreported CVEs. Every known CVE that matches an SBOM package on a running image has been reported by the scanner.'}
            </p>
          </div>
        ) : (
          <>
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-5 py-3 text-left text-caption font-bold tracking-[0.1em] uppercase text-text-tertiary">Severity</th>
                  <th className="px-5 py-3 text-left text-caption font-bold tracking-[0.1em] uppercase text-text-tertiary">CVE</th>
                  <th className="px-5 py-3 text-left text-caption font-bold tracking-[0.1em] uppercase text-text-tertiary">Packages</th>
                  <th className="px-5 py-3 text-left text-caption font-bold tracking-[0.1em] uppercase text-text-tertiary">Image</th>
                  <th className="px-5 py-3 text-left text-caption font-bold tracking-[0.1em] uppercase text-text-tertiary">Workloads</th>
                  <th className="px-5 py-3 text-left text-caption font-bold tracking-[0.1em] uppercase text-text-tertiary">Fix</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((cve) => (
                  <tr
                    key={`${cve.cve_id}-${cve.image_reference}`}
                    className="border-b border-border/50 last:border-b-0 hover:bg-bg-hover transition-colors align-top"
                  >
                    <td className="px-5 py-3.5">
                      <SeverityBadge severity={cve.severity} />
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <Link to={`/vulnerabilities/${cve.vulnerability_id}`} className="font-mono text-sm font-medium text-text-primary hover:text-accent">
                          {cve.cve_id}
                        </Link>
                        <ExploitabilityChips kevListed={cve.kev_listed} epssScore={cve.epss_score} />
                      </div>
                      {cve.title && (
                        <span className="block text-caption text-text-tertiary truncate max-w-[280px] mt-0.5">{cve.title}</span>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex flex-col gap-0.5">
                        {cve.packages.map(pkg => (
                          <span key={pkg} className="font-mono text-xs text-text-secondary">{pkg}</span>
                        ))}
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <ImageRef references={cve.image_reference} className="text-caption w-full" />
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex flex-col gap-0.5">
                        {cve.workloads.slice(0, 3).map(w => (
                          <span key={w} className="font-mono text-xs text-text-secondary">{w}</span>
                        ))}
                        {cve.workloads.length > 3 && (
                          <span className="text-caption text-text-tertiary" title={cve.workloads.slice(3).join('\n')}>
                            +{cve.workloads.length - 3} more
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="font-mono text-xs text-text-secondary">
                        {cve.fixed_versions.length ? cve.fixed_versions.join(', ') : '-'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <Pagination page={page} totalPages={result.total_pages} onPageChange={setPage} />
          </>
        )}
      </div>
    </>
  );
}
