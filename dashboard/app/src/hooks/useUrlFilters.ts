import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { VulnerabilityFilters } from '../types';

const DEFAULT_FILTERS: VulnerabilityFilters = {
  status: ['open', 'triaged'],
  page: 1,
  limit: 25,
  sort_by: 'first_detected',
  sort_order: 'desc'
};

/**
 * Hook for managing vulnerability filters in URL search params
 * Persists filter state across navigation and enables shareable URLs
 */
export function useUrlFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters = useMemo((): VulnerabilityFilters => {
    const severity = searchParams.get('severity');
    const status = searchParams.get('status');
    const namespace = searchParams.get('namespace');
    const scanner_type = searchParams.get('scanner_type');
    const search = searchParams.get('search');
    const page = searchParams.get('page');
    const limit = searchParams.get('limit');
    const sort_by = searchParams.get('sort_by');
    const sort_order = searchParams.get('sort_order');

    return {
      severity: severity ? severity.split(',') : undefined,
      status: status ? status.split(',') : DEFAULT_FILTERS.status,
      namespace: namespace || undefined,
      scanner_type: scanner_type || undefined,
      search: search || undefined,
      page: page ? parseInt(page, 10) : DEFAULT_FILTERS.page,
      limit: limit ? parseInt(limit, 10) : DEFAULT_FILTERS.limit,
      sort_by: (sort_by as VulnerabilityFilters['sort_by']) || DEFAULT_FILTERS.sort_by,
      sort_order: (sort_order as VulnerabilityFilters['sort_order']) || DEFAULT_FILTERS.sort_order
    };
  }, [searchParams]);

  const setFilters = useCallback((newFilters: VulnerabilityFilters) => {
    const params = new URLSearchParams();

    if (newFilters.severity?.length) {
      params.set('severity', newFilters.severity.join(','));
    }
    if (newFilters.status?.length) {
      // Only set if different from default
      const defaultStatus = DEFAULT_FILTERS.status?.join(',');
      const newStatus = newFilters.status.join(',');
      if (newStatus !== defaultStatus) {
        params.set('status', newStatus);
      }
    }
    if (newFilters.namespace) {
      params.set('namespace', newFilters.namespace);
    }
    if (newFilters.scanner_type) {
      params.set('scanner_type', newFilters.scanner_type);
    }
    if (newFilters.search) {
      params.set('search', newFilters.search);
    }
    if (newFilters.page && newFilters.page !== DEFAULT_FILTERS.page) {
      params.set('page', String(newFilters.page));
    }
    if (newFilters.limit && newFilters.limit !== DEFAULT_FILTERS.limit) {
      params.set('limit', String(newFilters.limit));
    }
    if (newFilters.sort_by && newFilters.sort_by !== DEFAULT_FILTERS.sort_by) {
      params.set('sort_by', newFilters.sort_by);
    }
    if (newFilters.sort_order && newFilters.sort_order !== DEFAULT_FILTERS.sort_order) {
      params.set('sort_order', newFilters.sort_order);
    }

    setSearchParams(params, { replace: true });
  }, [setSearchParams]);

  const clearFilters = useCallback(() => {
    setSearchParams({}, { replace: true });
  }, [setSearchParams]);

  const updateFilter = useCallback(<K extends keyof VulnerabilityFilters>(
    key: K,
    value: VulnerabilityFilters[K]
  ) => {
    setFilters({
      ...filters,
      [key]: value,
      // Reset to page 1 when changing filters (except when changing page itself)
      page: key === 'page' ? (value as number) : 1
    });
  }, [filters, setFilters]);

  // Check if any non-default filters are active
  const hasActiveFilters = useMemo(() => {
    return !!(
      filters.search ||
      filters.severity?.length ||
      filters.namespace ||
      filters.scanner_type ||
      (filters.status && filters.status.join(',') !== DEFAULT_FILTERS.status?.join(','))
    );
  }, [filters]);

  // Get list of active filter labels for display
  const activeFilterLabels = useMemo(() => {
    const labels: Array<{ key: keyof VulnerabilityFilters; label: string; value: string }> = [];

    if (filters.search) {
      labels.push({ key: 'search', label: 'Search', value: filters.search });
    }
    if (filters.severity?.length) {
      labels.push({ key: 'severity', label: 'Severity', value: filters.severity.join(', ') });
    }
    if (filters.status && filters.status.join(',') !== DEFAULT_FILTERS.status?.join(',')) {
      labels.push({ key: 'status', label: 'Status', value: filters.status.join(', ') });
    }
    if (filters.namespace) {
      labels.push({ key: 'namespace', label: 'Namespace', value: filters.namespace });
    }
    if (filters.scanner_type) {
      labels.push({ key: 'scanner_type', label: 'Scanner', value: filters.scanner_type });
    }

    return labels;
  }, [filters]);

  const removeFilter = useCallback((key: keyof VulnerabilityFilters) => {
    const newFilters = { ...filters };

    if (key === 'status') {
      newFilters.status = DEFAULT_FILTERS.status;
    } else {
      delete newFilters[key];
    }

    newFilters.page = 1;
    setFilters(newFilters);
  }, [filters, setFilters]);

  return {
    filters,
    setFilters,
    updateFilter,
    clearFilters,
    removeFilter,
    hasActiveFilters,
    activeFilterLabels,
    DEFAULT_FILTERS
  };
}
