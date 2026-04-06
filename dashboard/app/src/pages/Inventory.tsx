import { useState, useCallback, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, Package, ArrowUpDown } from 'lucide-react';
import { sbomService } from '../services/api';

export function Inventory() {
  const [searchValue, setSearchValue] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  const handleSearchChange = useCallback((value: string) => {
    setSearchValue(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setSearchQuery(value);
      setPage(0);
    }, 400);
  }, []);

  const { data: searchResults, isLoading: isSearching } = useQuery({
    queryKey: ['package-search', searchQuery, page],
    queryFn: () => sbomService.searchPackages(searchQuery, undefined, page),
    enabled: searchQuery.length >= 2,
  });

  const { data: topPackages, isLoading: isLoadingTop } = useQuery({
    queryKey: ['top-packages'],
    queryFn: () => sbomService.getTopPackages(20),
    enabled: searchQuery.length < 2,
  });

  const showSearch = searchQuery.length >= 2;
  const results = showSearch ? searchResults?.content : topPackages?.content;
  const totalElements = showSearch ? searchResults?.total_elements : topPackages?.total_elements;
  const totalPages = showSearch ? searchResults?.total_pages : 1;
  const isLoading = showSearch ? isSearching : isLoadingTop;

  return (
    <div className="space-y-3 max-w-[1400px] mx-auto">
      <div className="flex items-start justify-between animate-fade-up">
        <div>
          <h1 className="font-display text-2xl italic text-text-primary tracking-tight">
            Software Inventory
          </h1>
          {totalElements !== undefined && (
            <p className="text-[13px] text-text-secondary mt-1">
              {showSearch
                ? `${totalElements.toLocaleString()} packages matching "${searchQuery}"`
                : `Top ${results?.length || 0} packages across fleet`}
            </p>
          )}
        </div>
      </div>

      <div className="bg-bg-secondary border border-border rounded-2xl px-[18px] py-3.5 flex items-center gap-3 animate-fade-up delay-1">
        <div className="flex items-center gap-2 bg-bg-tertiary border border-border rounded-lg px-3 py-2 min-w-[300px] flex-1">
          <Search className="h-3.5 w-3.5 text-text-tertiary flex-shrink-0" />
          <input
            type="text"
            placeholder="Search packages across fleet (e.g. openssl, log4j, curl)..."
            value={searchValue}
            onChange={(e) => handleSearchChange(e.target.value)}
            className="bg-transparent border-none outline-none text-[13px] text-text-primary w-full placeholder:text-text-tertiary"
          />
        </div>
      </div>

      <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden animate-fade-up delay-2">
        {isLoading ? (
          <div className="px-6 py-12 text-center">
            <div className="inline-block h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : !results?.length ? (
          <div className="px-6 py-12 text-center">
            <Package className="h-8 w-8 text-text-tertiary mx-auto mb-3" />
            <p className="text-sm text-text-tertiary">
              {searchQuery.length >= 2
                ? `No packages found matching "${searchQuery}"`
                : 'No packages indexed yet. Ingest SBOMs to populate the inventory.'}
            </p>
          </div>
        ) : (
          <>
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                    Package
                  </th>
                  <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                    Version
                  </th>
                  <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                    Type
                  </th>
                  {showSearch ? (
                    <>
                      <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                        License
                      </th>
                      <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                        Image
                      </th>
                    </>
                  ) : (
                    <th className="px-5 py-3 text-right text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                      <span className="inline-flex items-center gap-1">
                        Images <ArrowUpDown className="h-3 w-3" />
                      </span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {results.map((pkg, i) => (
                  <tr
                    key={`${pkg.name}-${pkg.version}-${i}`}
                    className="border-b border-border/50 last:border-b-0 hover:bg-bg-hover transition-colors"
                  >
                    <td className="px-5 py-3.5">
                      <span className="font-mono text-[13px] font-medium text-text-primary">
                        {pkg.name}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="font-mono text-[12px] text-text-secondary">
                        {pkg.version}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      {pkg.type && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-bg-tertiary text-text-secondary border border-border">
                          {pkg.type}
                        </span>
                      )}
                    </td>
                    {showSearch ? (
                      <>
                        <td className="px-5 py-3.5 text-[12px] text-text-secondary">
                          {(pkg as any).license || '-'}
                        </td>
                        <td className="px-5 py-3.5">
                          <span className="font-mono text-[11px] text-text-tertiary truncate block max-w-[250px]">
                            {(pkg as any).image_reference}
                          </span>
                        </td>
                      </>
                    ) : (
                      <td className="px-5 py-3.5 text-right">
                        <span className="font-mono text-[13px] font-medium text-accent">
                          {(pkg as any).image_count}
                        </span>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>

            {showSearch && totalPages !== undefined && totalPages > 1 && (
              <div className="flex items-center justify-between px-5 py-3 border-t border-border">
                <span className="text-[12px] text-text-tertiary">
                  Page {page + 1} of {totalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-bg-tertiary text-text-secondary border border-border hover:border-accent disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(p => p + 1)}
                    disabled={page + 1 >= totalPages}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-bg-tertiary text-text-secondary border border-border hover:border-accent disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
