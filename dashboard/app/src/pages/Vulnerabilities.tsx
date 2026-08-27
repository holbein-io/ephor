import { useEffect, useState } from 'react';
import { VulnerabilityTable, type Density } from '../components/VulnerabilityTable';
import { VulnerabilityFilters } from '../components/VulnerabilityFilters';
import { FilterPills } from '../components/FilterPills';
import { FilterPresets } from '../components/FilterPresets';
import { useUrlFilters } from '../hooks/useUrlFilters';
import { useVulnerabilityList } from '../contexts/VulnerabilityListContext';
import { VulnerabilityFilters as FiltersType } from '../types';
import { Rows3, Rows4 } from 'lucide-react';

export function Vulnerabilities() {
  const {
    filters,
    setFilters,
    clearFilters,
    removeFilter,
    hasActiveFilters,
    activeFilterLabels
  } = useUrlFilters();

  const {
    setQueryFilters,
    allVulnerabilities,
    isLoading,
    error,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    totalCount
  } = useVulnerabilityList();

  const [density, setDensity] = useState<Density>(() => {
    return (localStorage.getItem('vuln-density') as Density) || 'comfortable';
  });

  useEffect(() => {
    setQueryFilters(filters);
  }, [filters, setQueryFilters]);

  useEffect(() => {
    localStorage.setItem('vuln-density', density);
  }, [density]);

  const handleFiltersChange = (newFilters: typeof filters) => {
    setFilters(newFilters);
  };

  const handleApplyPreset = (presetFilters: Partial<FiltersType>) => {
    setFilters({ ...filters, ...presetFilters });
  };

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-danger">Failed to load vulnerabilities. Please try again.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-start justify-between animate-fade-up">
        <div>
          <h1 className="font-display text-2xl italic text-text-primary tracking-tight">
            Vulnerability Index
          </h1>
          {totalCount !== undefined && (
            <p className="text-sm text-text-secondary mt-1">
              {totalCount.toLocaleString()} vulnerabilities tracked
            </p>
          )}
        </div>
        <div className="flex items-center gap-2.5">
          <FilterPresets
            currentFilters={filters}
            onApplyPreset={handleApplyPreset}
          />
          <div className="flex items-center gap-0.5 bg-bg-tertiary rounded-lg p-0.5" role="group" aria-label="Row density">
            <button
              onClick={() => setDensity('comfortable')}
              aria-pressed={density === 'comfortable'}
              className={`p-1.5 rounded-md transition-colors ${
                density === 'comfortable'
                  ? 'bg-bg-card text-accent shadow-sm'
                  : 'text-text-tertiary hover:text-text-secondary'
              }`}
              title="Comfortable rows"
            >
              <Rows3 className="h-4 w-4" />
            </button>
            <button
              onClick={() => setDensity('compact')}
              aria-pressed={density === 'compact'}
              className={`p-1.5 rounded-md transition-colors ${
                density === 'compact'
                  ? 'bg-bg-card text-accent shadow-sm'
                  : 'text-text-tertiary hover:text-text-secondary'
              }`}
              title="Compact rows"
            >
              <Rows4 className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      <div className="animate-fade-up delay-1">
        <VulnerabilityFilters
          filters={filters}
          onFiltersChange={handleFiltersChange}
          loading={isLoading}
        />
      </div>

      {hasActiveFilters && (
        <div className="animate-fade-up delay-2">
          <FilterPills
            filters={activeFilterLabels}
            onRemove={removeFilter}
            onClearAll={clearFilters}
          />
        </div>
      )}

      {totalCount !== undefined && (
        <div className="flex items-center justify-between text-xs text-text-tertiary animate-fade-up delay-2">
          <span>
            Showing {allVulnerabilities.length.toLocaleString()} of{' '}
            <span className="font-medium text-text-secondary">{totalCount.toLocaleString()}</span>
          </span>
        </div>
      )}

      <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden animate-fade-up delay-3">
        <VulnerabilityTable
          vulnerabilities={allVulnerabilities}
          loading={isLoading}
          density={density}
          hasNextPage={hasNextPage}
          isFetchingNextPage={isFetchingNextPage}
          fetchNextPage={fetchNextPage}
          totalCount={totalCount}
          onClearFilters={hasActiveFilters ? clearFilters : undefined}
        />
      </div>
    </div>
  );
}
