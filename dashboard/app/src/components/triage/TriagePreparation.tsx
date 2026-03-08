import { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Plus, CheckCircle2, Flag, MessageSquare, ExternalLink, AlertCircle, Search, Loader2, Filter, Maximize2, Minimize2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Vulnerability, TriagePreparation as TriagePreparationType } from '../../types';
import { SEVERITY_COLORS } from '../../constants/colors';
import { formatRelativeTime, debounce } from '../../utils';
import { useInfiniteTriageVulnerabilities } from '../../hooks/useInfiniteTriageVulnerabilities';
import { dashboardService } from '../../services/api';

interface TriagePreparationProps {
  preparations: TriagePreparationType[] | undefined;
  sessionId: number;
  onAddToSession: (vulnerability: Vulnerability, priority?: string, notes?: string) => void;
  maximized?: boolean;
  onToggleMaximize?: () => void;
}

const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'] as const;

export function TriagePreparation({
  preparations,
  onAddToSession,
  maximized = false,
  onToggleMaximize,
}: TriagePreparationProps) {
  const [selectedSeverities, setSelectedSeverities] = useState<string[]>(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']);
  const [searchInput, setSearchInput] = useState('');
  const [searchFilter, setSearchFilter] = useState('');
  const [namespace, setNamespaceRaw] = useState('');
  const setNamespace = (v: string) => setNamespaceRaw(v === 'all' ? '' : v);
  const [sortBy, setSortBy] = useState<'severity' | 'first_detected' | 'last_seen' | 'cve_id'>('severity');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [showFilters, setShowFilters] = useState(false);
  const [notesInputVulnId, setNotesInputVulnId] = useState<number | null>(null);
  const [notesText, setNotesText] = useState('');

  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Debounced search
  const debouncedSetSearch = useMemo(
    () => debounce((value: string) => setSearchFilter(value), 300),
    []
  );

  const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchInput(e.target.value);
    debouncedSetSearch(e.target.value);
  }, [debouncedSetSearch]);

  // Build filters for the query
  const filters = useMemo(() => ({
    severity: selectedSeverities.length > 0 ? selectedSeverities : undefined,
    status: ['open'] as string[],
    namespace: namespace || undefined,
    search: searchFilter || undefined,
    sort_by: sortBy,
    sort_order: sortOrder
  }), [selectedSeverities, namespace, searchFilter, sortBy, sortOrder]);

  const {
    vulnerabilities,
    isLoading,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    totalCount
  } = useInfiniteTriageVulnerabilities(filters);

  // Fetch namespaces for filter
  const { data: namespaces } = useQuery({
    queryKey: ['dashboard-namespaces'],
    queryFn: () => dashboardService.getNamespaces()
  });

  // Infinite scroll handler
  const handleScroll = useCallback(() => {
    if (!scrollContainerRef.current || !hasNextPage || isFetchingNextPage) return;

    const { scrollTop, scrollHeight, clientHeight } = scrollContainerRef.current;
    const scrolledPercentage = (scrollTop + clientHeight) / scrollHeight;

    if (scrolledPercentage > 0.8) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  useEffect(() => {
    const element = scrollContainerRef.current;
    if (!element) return;

    element.addEventListener('scroll', handleScroll);
    return () => element.removeEventListener('scroll', handleScroll);
  }, [handleScroll]);

  const toggleSeverity = (severity: string) => {
    setSelectedSeverities(prev =>
      prev.includes(severity)
        ? prev.filter(s => s !== severity)
        : [...prev, severity]
    );
  };

  const isVulnerabilityAdded = (vulnId: number): boolean => {
    return preparations?.some(p => p.vulnerability_id === vulnId) || false;
  };

  const getSeverityColor = (severity: string) => {
    return SEVERITY_COLORS[severity as keyof typeof SEVERITY_COLORS] || SEVERITY_COLORS.UNKNOWN;
  };

  const handleOpenNotes = (vulnId: number) => {
    setNotesInputVulnId(vulnId);
    setNotesText('');
  };

  const handleCancelNotes = () => {
    setNotesInputVulnId(null);
    setNotesText('');
  };

  const handleAddWithNotes = (vuln: Vulnerability) => {
    if (!isVulnerabilityAdded(vuln.id!)) {
      onAddToSession(vuln, 'medium', notesText);
    }
    setNotesInputVulnId(null);
    setNotesText('');
  };

  return (
    <Card>
      <CardHeader>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <AlertCircle className="w-5 h-5" />
              Vulnerability Preparation
              {totalCount !== undefined && (
                <span className="text-sm font-normal text-text-tertiary">
                  ({totalCount} total)
                </span>
              )}
            </CardTitle>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={() => setShowFilters(!showFilters)}
              >
                <Filter className="w-4 h-4 mr-1" />
                Filters
              </Button>
              {onToggleMaximize && (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={onToggleMaximize}
                  title={maximized ? 'Restore layout' : 'Maximize'}
                >
                  {maximized ? (
                    <Minimize2 className="w-4 h-4" />
                  ) : (
                    <Maximize2 className="w-4 h-4" />
                  )}
                </Button>
              )}
            </div>
          </div>

          {/* Severity toggle buttons */}
          <div className="flex gap-2 flex-wrap">
            {SEVERITIES.map(severity => {
              const color = getSeverityColor(severity);
              const isSelected = selectedSeverities.includes(severity);

              return (
                <Button
                  key={severity}
                  size="sm"
                  variant={isSelected ? "primary" : "outline"}
                  onClick={() => toggleSeverity(severity)}
                  className={isSelected ? '' : color.tailwind}
                >
                  {severity}
                </Button>
              );
            })}
          </div>

          {/* Search input */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-tertiary" />
            <Input
              placeholder="Search by CVE ID, package name..."
              value={searchInput}
              onChange={handleSearchChange}
              className="pl-9"
            />
          </div>

          {/* Expandable filters */}
          {showFilters && (
            <div className="grid grid-cols-3 gap-3 pt-2 border-t">
              <div>
                <label className="text-xs text-text-tertiary mb-1 block">Namespace</label>
                <Select value={namespace} onValueChange={setNamespace}>
                  <SelectTrigger>
                    <SelectValue placeholder="All namespaces" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All namespaces</SelectItem>
                    {namespaces?.map(ns => (
                      <SelectItem key={ns} value={ns}>{ns}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <label className="text-xs text-text-tertiary mb-1 block">Sort by</label>
                <Select value={sortBy} onValueChange={(v) => setSortBy(v as typeof sortBy)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="severity">Severity</SelectItem>
                    <SelectItem value="first_detected">First detected</SelectItem>
                    <SelectItem value="last_seen">Last seen</SelectItem>
                    <SelectItem value="cve_id">CVE ID</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <label className="text-xs text-text-tertiary mb-1 block">Order</label>
                <Select value={sortOrder} onValueChange={(v) => setSortOrder(v as typeof sortOrder)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="desc">Descending</SelectItem>
                    <SelectItem value="asc">Ascending</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="w-5 h-5 animate-spin text-text-tertiary mr-2" />
            <span className="text-text-tertiary">Loading vulnerabilities...</span>
          </div>
        ) : !vulnerabilities || vulnerabilities.length === 0 ? (
          <div className="text-center py-8 text-text-tertiary">
            <p>No vulnerabilities found matching the selected criteria.</p>
          </div>
        ) : (
          <div
            ref={scrollContainerRef}
            className="space-y-4 overflow-y-auto"
            style={{ maxHeight: maximized ? 'calc(100vh - 280px)' : '600px' }}
          >
            {vulnerabilities.map((vuln: Vulnerability & { affected_workloads: number }) => {
              const added = isVulnerabilityAdded(vuln.id!);
              const severityColor = getSeverityColor(vuln.severity);

              return (
                <div
                  key={vuln.id}
                  className="border rounded-lg p-4 hover:shadow-sm transition-shadow"
                >
                  {/* Header */}
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-start gap-3 flex-1">
                      <Badge
                        variant="info"
                        className={`text-xs ${severityColor.tailwind}`}
                      >
                        {vuln.severity}
                      </Badge>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <h4 className="font-medium text-text-primary">
                            {vuln.cve_id}
                          </h4>
                          {vuln.primary_url && (
                            <a
                              href={vuln.primary_url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-accent hover:text-accent-hover"
                            >
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          )}
                        </div>
                        <p className="text-sm text-text-secondary mt-1">
                          {vuln.package_name}
                          {vuln.package_version && ` v${vuln.package_version}`}
                        </p>
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant={added ? "outline" : "primary"}
                        onClick={() => {
                          if (!added) onAddToSession(vuln);
                        }}
                        disabled={added}
                        className={added ? 'bg-success/15 text-success border-success/30' : ''}
                      >
                        {added ? (
                          <>
                            <CheckCircle2 className="w-4 h-4 mr-1" />
                            Added
                          </>
                        ) : (
                          <>
                            <Plus className="w-4 h-4 mr-1" />
                            Add
                          </>
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          if (!added) onAddToSession(vuln, 'high', 'Flagged as high priority');
                        }}
                        disabled={added}
                      >
                        <Flag className="w-4 h-4 mr-1" />
                        High Priority
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOpenNotes(vuln.id!)}
                        disabled={added || notesInputVulnId === vuln.id}
                      >
                        <MessageSquare className="w-4 h-4 mr-1" />
                        Notes
                      </Button>
                    </div>
                  </div>

                  {/* Inline Notes Input */}
                  {notesInputVulnId === vuln.id && (
                    <div className="mt-3 p-3 bg-accent/10 border border-accent/30 rounded-lg">
                      <label className="block text-sm font-medium text-accent-hover mb-2">
                        Preparation Notes
                      </label>
                      <textarea
                        value={notesText}
                        onChange={(e) => setNotesText(e.target.value)}
                        placeholder="Add notes for the triage session..."
                        className="w-full px-3 py-2 border border-accent/40 rounded-md text-sm focus:ring-accent focus:border-accent"
                        rows={3}
                        autoFocus
                      />
                      <div className="flex justify-end gap-2 mt-2">
                        <Button size="sm" variant="outline" onClick={handleCancelNotes}>
                          Cancel
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => handleAddWithNotes(vuln)}
                          disabled={!notesText.trim()}
                        >
                          <Plus className="w-4 h-4 mr-1" />
                          Add with Notes
                        </Button>
                      </div>
                    </div>
                  )}

                  {/* Vulnerability Details */}
                  {vuln.title && (
                    <p className="text-sm text-text-secondary mb-2">{vuln.title}</p>
                  )}
                  {vuln.description && (
                    <p className="text-xs text-text-tertiary mb-3 line-clamp-2">
                      {vuln.description}
                    </p>
                  )}

                  {/* Metadata */}
                  <div className="flex items-center gap-4 text-xs text-text-tertiary">
                    <span>First detected: {formatRelativeTime(vuln.first_detected)}</span>
                    <span>Last seen: {formatRelativeTime(vuln.last_seen)}</span>
                    {vuln.fixed_version && (
                      <span className="text-success">
                        Fix available: v{vuln.fixed_version}
                      </span>
                    )}
                    {vuln.affected_workloads > 0 && (
                      <span>Affected workloads: {vuln.affected_workloads}</span>
                    )}
                  </div>
                </div>
              );
            })}

            {/* Infinite scroll footer */}
            {isFetchingNextPage && (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="w-4 h-4 animate-spin text-text-tertiary mr-2" />
                <span className="text-sm text-text-tertiary">Loading more...</span>
              </div>
            )}
            {!hasNextPage && vulnerabilities.length > 0 && (
              <div className="text-center py-3 text-xs text-text-tertiary">
                Loaded {vulnerabilities.length} of {totalCount ?? vulnerabilities.length}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
