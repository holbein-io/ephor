import { useInfiniteQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { vulnerabilityService } from '../services/api';
import { VulnerabilityFilters } from '../types';

const PAGE_SIZE = 30;

type TriageQueryFilters = Omit<VulnerabilityFilters, 'page' | 'limit'>;

export function useInfiniteTriageVulnerabilities(filters: TriageQueryFilters) {
  const {
    data: infiniteData,
    isLoading,
    error,
    hasNextPage: rawHasNextPage,
    isFetchingNextPage,
    fetchNextPage
  } = useInfiniteQuery({
    queryKey: ['triage-vulnerabilities-infinite', filters],
    queryFn: async ({ pageParam = 1 }) => {
      const response = await vulnerabilityService.getVulnerabilities({
        ...filters,
        page: pageParam,
        limit: PAGE_SIZE
      });
      return response;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      const { page, total_pages } = lastPage.pagination;
      return page < total_pages ? page + 1 : undefined;
    }
  });

  const hasNextPage = rawHasNextPage ?? false;

  const allVulnerabilities = useMemo(() => {
    if (!infiniteData?.pages) return [];
    return infiniteData.pages.flatMap(page => page.data);
  }, [infiniteData?.pages]);

  const totalCount = infiniteData?.pages[0]?.pagination.total;

  return {
    vulnerabilities: allVulnerabilities,
    isLoading,
    error: error as Error | null,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    totalCount
  };
}
