import { useEffect, useState, useMemo } from 'react';
import { VulnerabilityTable } from '../components/VulnerabilityTable';
import { VirtualVulnerabilityTable } from '../components/VirtualVulnerabilityTable';
import { VulnerabilityFilters } from '../components/VulnerabilityFilters';
import { FilterPills } from '../components/FilterPills';
import { FilterPresets } from '../components/FilterPresets';
import { useUrlFilters } from '../hooks/useUrlFilters';
import { useVulnerabilityList } from '../contexts/VulnerabilityListContext';
import { VulnerabilityFilters as FiltersType } from '../types';
import { List, Table2 } from 'lucide-react';

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

  const [viewMode, setViewMode] = useState<'table' | 'infinite'>(() => {
    return (localStorage.getItem('vuln-view-mode') as 'table' | 'infinite') || 'infinite';
  });

  const infiniteFilters = useMemo(() => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { page, limit, ...rest } = filters;
    return rest;
  }, [filters]);

  useEffect(() => {
    setQueryFilters(infiniteFilters);
  }, [infiniteFilters, setQueryFilters]);

  useEffect(() => {
    localStorage.setItem('vuln-view-mode', viewMode);
  }, [viewMode]);

  const handleFiltersChange = (newFilters: typeof filters) => {
    setFilters(newFilters);
  };

  const handleApplyPreset = (presetFilters: Partial<FiltersType>) => {
    setFilters({ ...filters, ...presetFilters, page: 1 });
  };

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-danger">Failed to load vulnerabilities. Please try again.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3 max-w-[1400px] mx-auto">
      {/* Page Header */}
      <div className="flex items-start justify-between animate-fade-up">
        <div>
          <h1 className="font-display text-2xl italic text-text-primary tracking-tight">
            Vulnerability Index
          </h1>
          {totalCount !== undefined && (
            <p className="text-[13px] text-text-secondary mt-1">
              {totalCount.toLocaleString()} findings across active workloads
            </p>
          )}
        </div>
        <div className="flex items-center gap-2.5">
          <FilterPresets
            currentFilters={filters}
            onApplyPreset={handleApplyPreset}
          />
          {/* View Mode Toggle */}
          <div className="flex items-center gap-0.5 bg-bg-tertiary rounded-lg p-0.5">
            <button
              onClick={() => setViewMode('infinite')}
              className={`p-1.5 rounded-md transition-colors ${
                viewMode === 'infinite'
                  ? 'bg-bg-card text-accent shadow-sm'
                  : 'text-text-tertiary hover:text-text-secondary'
              }`}
              title="Infinite scroll view"
            >
              <List className="h-4 w-4" />
            </button>
            <button
              onClick={() => setViewMode('table')}
              className={`p-1.5 rounded-md transition-colors ${
                viewMode === 'table'
                  ? 'bg-bg-card text-accent shadow-sm'
                  : 'text-text-tertiary hover:text-text-secondary'
              }`}
              title="Paginated table view"
            >
              <Table2 className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="animate-fade-up delay-1">
        <VulnerabilityFilters
          filters={filters}
          onFiltersChange={handleFiltersChange}
          loading={isLoading}
        />
      </div>

      {/* Active Filter Pills */}
      {hasActiveFilters && (
        <div className="animate-fade-up delay-2">
          <FilterPills
            filters={activeFilterLabels}
            onRemove={removeFilter}
            onClearAll={clearFilters}
          />
        </div>
      )}

      {/* Results bar */}
      {totalCount !== undefined && (
        <div className="flex items-center justify-between text-[12.5px] text-text-tertiary animate-fade-up delay-2">
          <span>
            Showing {allVulnerabilities.length.toLocaleString()} of{' '}
            <span className="font-medium text-text-secondary">{totalCount.toLocaleString()}</span>
          </span>
        </div>
      )}

      {/* Results Table */}
      <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden animate-fade-up delay-3">
        {viewMode === 'infinite' ? (
          <VirtualVulnerabilityTable
            vulnerabilities={allVulnerabilities as any}
            loading={isLoading}
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            fetchNextPage={fetchNextPage}
            totalCount={totalCount}
          />
        ) : (
          <VulnerabilityTable
            vulnerabilities={allVulnerabilities as any}
            loading={isLoading}
          />
        )}
      </div>
    </div>
  );
}
