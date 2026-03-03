import { useEffect, useState, useMemo } from 'react';
import { VulnerabilityTable } from '../components/VulnerabilityTable';
import { VirtualVulnerabilityTable } from '../components/VirtualVulnerabilityTable';
import { VulnerabilityFilters } from '../components/VulnerabilityFilters';
import { FilterPills } from '../components/FilterPills';
import { FilterPresets } from '../components/FilterPresets';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { useUrlFilters } from '../hooks/useUrlFilters';
import { useVulnerabilityList } from '../contexts/VulnerabilityListContext';
import { useInfiniteVulnerabilities } from '../hooks/useInfiniteVulnerabilities';
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

  const { setListItems, setPageLoader } = useVulnerabilityList();
  const [viewMode, setViewMode] = useState<'table' | 'infinite'>(() => {
    return (localStorage.getItem('vuln-view-mode') as 'table' | 'infinite') || 'infinite';
  });

  const infiniteFilters = useMemo(() => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { page, limit, ...rest } = filters;
    return rest;
  }, [filters]);

  const {
    data: infiniteData,
    isLoading: infiniteLoading,
    error: infiniteError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage
  } = useInfiniteVulnerabilities(infiniteFilters);

  const allVulnerabilities = useMemo(() => {
    if (!infiniteData?.pages) return [];
    return infiniteData.pages.flatMap(page => page.data);
  }, [infiniteData?.pages]);

  const totalCount = infiniteData?.pages[0]?.pagination.total;

  useEffect(() => {
    if (allVulnerabilities.length > 0) {
      setListItems(allVulnerabilities.map((v: any) => ({ id: v.id, cve_id: v.cve_id })));
    }
  }, [allVulnerabilities, setListItems]);

  useEffect(() => {
    setPageLoader({
      fetchNextPage: () => fetchNextPage(),
      hasNextPage: hasNextPage ?? false
    });
    return () => setPageLoader(null);
  }, [fetchNextPage, hasNextPage, setPageLoader]);

  useEffect(() => {
    localStorage.setItem('vuln-view-mode', viewMode);
  }, [viewMode]);

  const handleFiltersChange = (newFilters: typeof filters) => {
    setFilters(newFilters);
  };

  const handleApplyPreset = (presetFilters: Partial<FiltersType>) => {
    setFilters({
      ...filters,
      ...presetFilters,
      page: 1
    });
  };

  const isLoading = infiniteLoading;
  const error = infiniteError;

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-danger">Failed to load vulnerabilities. Please try again.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Summary Stats */}
      {totalCount !== undefined && (
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-text-secondary">Total Vulnerabilities</p>
                <p className="text-2xl font-bold text-text-primary">{totalCount}</p>
              </div>
              <div className="flex items-center gap-4">
                {/* View Mode Toggle */}
                <div className="flex items-center gap-1 bg-bg-tertiary rounded-lg p-1">
                  <button
                    onClick={() => setViewMode('infinite')}
                    className={`p-1.5 rounded-md transition-colors ${
                      viewMode === 'infinite'
                        ? 'bg-bg-card shadow text-accent'
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
                        ? 'bg-bg-card shadow text-accent'
                        : 'text-text-tertiary hover:text-text-secondary'
                    }`}
                    title="Paginated table view"
                  >
                    <Table2 className="h-4 w-4" />
                  </button>
                </div>
                <div className="text-right text-sm text-text-secondary">
                  Loaded {allVulnerabilities.length} of {totalCount}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Active Filter Pills */}
      {hasActiveFilters && (
        <FilterPills
          filters={activeFilterLabels}
          onRemove={removeFilter}
          onClearAll={clearFilters}
        />
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Filter Vulnerabilities</CardTitle>
            <FilterPresets
              currentFilters={filters}
              onApplyPreset={handleApplyPreset}
            />
          </div>
        </CardHeader>
        <CardContent>
          <VulnerabilityFilters
            filters={filters}
            onFiltersChange={handleFiltersChange}
            loading={isLoading}
          />
        </CardContent>
      </Card>

      {/* Results */}
      <Card>
        <CardHeader>
          <CardTitle>
            Vulnerabilities
            {totalCount !== undefined && (
              <span className="ml-2 text-sm font-normal text-text-tertiary">
                ({totalCount} total)
              </span>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
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
        </CardContent>
      </Card>
    </div>
  );
}
