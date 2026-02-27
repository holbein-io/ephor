import { useInfiniteQuery } from '@tanstack/react-query';
import { vulnerabilityService } from '../services/api';
import { VulnerabilityFilters } from '../types';

const PAGE_SIZE = 50;

/**
 * Hook for infinite scroll pagination of vulnerabilities
 * Fetches pages of vulnerabilities as user scrolls
 */
export function useInfiniteVulnerabilities(filters: Omit<VulnerabilityFilters, 'page' | 'limit'>) {
  return useInfiniteQuery({
    queryKey: ['vulnerabilities-infinite', filters],
    queryFn: async ({ pageParam = 1 }) => {
      const response = await vulnerabilityService.getVulnerabilities({
        ...filters,
        page: pageParam,
        limit: PAGE_SIZE
      } as any);
      return response;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      const { page, total_pages } = lastPage.pagination;
      return page < total_pages ? page + 1 : undefined;
    },
    getPreviousPageParam: (firstPage) => {
      const { page } = firstPage.pagination;
      return page > 1 ? page - 1 : undefined;
    }
  });
}
