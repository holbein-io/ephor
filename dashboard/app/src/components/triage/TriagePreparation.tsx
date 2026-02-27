import { useState } from 'react';
import { Plus, CheckCircle2, Flag, MessageSquare, ExternalLink, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Vulnerability, TriagePreparation as TriagePreparationType } from '../../types';
import { SEVERITY_COLORS } from '../../constants/colors';
import { formatRelativeTime } from '../../utils';

interface GroupedVulnerability {
  cveId: string;
  vulnerabilities: Vulnerability[];
  totalWorkloads: number;
  uniqueVersions: string[];
  representative: Vulnerability;
}

interface TriagePreparationProps {
  vulnerabilities: GroupedVulnerability[] | undefined;
  preparations: TriagePreparationType[] | undefined;
  isLoading: boolean;
  sessionId: number;
  selectedSeverities: string[];
  onToggleSeverity: (severity: string) => void;
  onAddToSession: (vulnerability: Vulnerability, priority?: string, notes?: string) => void;
}

export function TriagePreparation({
  vulnerabilities,
  preparations,
  isLoading,
  selectedSeverities,
  onToggleSeverity,
  onAddToSession,
}: TriagePreparationProps) {
  const [expandedCves, setExpandedCves] = useState<Set<string>>(new Set());
  const [notesInputCve, setNotesInputCve] = useState<string | null>(null);
  const [notesText, setNotesText] = useState('');

  const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];

  const handleOpenNotesInput = (cveId: string) => {
    setNotesInputCve(cveId);
    setNotesText('');
  };

  const handleCancelNotes = () => {
    setNotesInputCve(null);
    setNotesText('');
  };

  const handleAddWithNotes = (group: GroupedVulnerability) => {
    group.vulnerabilities.forEach(v => {
      if (!isVulnerabilityAdded(v.id!)) {
        onAddToSession(v, 'medium', notesText);
      }
    });
    setNotesInputCve(null);
    setNotesText('');
  };

  const toggleExpanded = (cveId: string) => {
    setExpandedCves(prev => {
      const newSet = new Set(prev);
      if (newSet.has(cveId)) {
        newSet.delete(cveId);
      } else {
        newSet.add(cveId);
      }
      return newSet;
    });
  };

  const isVulnerabilityAdded = (vulnId: number): boolean => {
    return preparations?.some(p => p.vulnerability_id === vulnId) || false;
  };

  const getSeverityColor = (severity: string) => {
    return SEVERITY_COLORS[severity as keyof typeof SEVERITY_COLORS] || SEVERITY_COLORS.UNKNOWN;
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <div className="text-text-tertiary">Loading vulnerabilities...</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5" />
            Vulnerability Preparation
          </CardTitle>
          <div className="flex gap-2">
            {severities.map(severity => {
              const color = getSeverityColor(severity);
              const isSelected = selectedSeverities.includes(severity);

              return (
                <Button
                  key={severity}
                  size="sm"
                  variant={isSelected ? "primary" : "outline"}
                  onClick={() => onToggleSeverity(severity)}
                  className={isSelected ? '' : color.tailwind}
                >
                  {severity}
                </Button>
              );
            })}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {!vulnerabilities || vulnerabilities.length === 0 ? (
          <div className="text-center py-8 text-text-tertiary">
            <p>No vulnerabilities found matching the selected criteria.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {vulnerabilities.map((group) => {
              const isExpanded = expandedCves.has(group.cveId);
              const allAdded = group.vulnerabilities.every(v => isVulnerabilityAdded(v.id!));
              const severityColor = getSeverityColor(group.representative.severity);

              return (
                <div
                  key={group.cveId}
                  className="border rounded-lg p-4 hover:shadow-sm transition-shadow"
                >
                  {/* Header */}
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-start gap-3 flex-1">
                      <Badge
                        variant="info"
                        className={`text-xs ${severityColor.tailwind}`}
                      >
                        {group.representative.severity}
                      </Badge>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <h4 className="font-medium text-text-primary">
                            {group.cveId}
                          </h4>
                          {group.representative.primary_url && (
                            <a
                              href={group.representative.primary_url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-accent hover:text-accent-hover"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          )}
                        </div>
                        <p className="text-sm text-text-secondary mt-1">
                          {group.representative.package_name}
                          {group.representative.package_version && ` v${group.representative.package_version}`}
                        </p>
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant={allAdded ? "outline" : "primary"}
                        onClick={() => {
                          group.vulnerabilities.forEach(v => {
                            if (!isVulnerabilityAdded(v.id!)) {
                              onAddToSession(v);
                            }
                          });
                        }}
                        disabled={allAdded}
                        className={allAdded ? 'bg-success/15 text-success border-success/30' : ''}
                      >
                        {allAdded ? (
                          <>
                            <CheckCircle2 className="w-4 h-4 mr-1" />
                            Added
                          </>
                        ) : (
                          <>
                            <Plus className="w-4 h-4 mr-1" />
                            Add {group.vulnerabilities.length > 1 ? `All (${group.vulnerabilities.length})` : ''}
                          </>
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          group.vulnerabilities.forEach(v => {
                            if (!isVulnerabilityAdded(v.id!)) {
                              onAddToSession(v, 'high', 'Flagged as high priority');
                            }
                          });
                        }}
                        disabled={allAdded}
                      >
                        <Flag className="w-4 h-4 mr-1" />
                        High Priority
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOpenNotesInput(group.cveId)}
                        disabled={allAdded || notesInputCve === group.cveId}
                      >
                        <MessageSquare className="w-4 h-4 mr-1" />
                        Add Notes
                      </Button>
                    </div>
                  </div>

                  {/* Inline Notes Input */}
                  {notesInputCve === group.cveId && (
                    <div className="mt-3 p-3 bg-accent/10 border border-accent/30 rounded-lg">
                      <label className="block text-sm font-medium text-accent-hover mb-2">
                        Preparation Notes
                      </label>
                      <textarea
                        value={notesText}
                        onChange={(e) => setNotesText(e.target.value)}
                        placeholder="Add notes for the triage session (e.g., context, priority reasoning, related tickets)..."
                        className="w-full px-3 py-2 border border-accent/40 rounded-md text-sm focus:ring-accent focus:border-accent"
                        rows={3}
                        autoFocus
                      />
                      <div className="flex justify-end gap-2 mt-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={handleCancelNotes}
                        >
                          Cancel
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => handleAddWithNotes(group)}
                          disabled={!notesText.trim()}
                        >
                          <Plus className="w-4 h-4 mr-1" />
                          Add with Notes
                        </Button>
                      </div>
                    </div>
                  )}

                  {/* CVE Details */}
                  {group.representative.title && (
                    <p className="text-sm text-text-secondary mb-2">{group.representative.title}</p>
                  )}
                  {group.representative.description && (
                    <p className="text-xs text-text-tertiary mb-3 line-clamp-2">
                      {group.representative.description}
                    </p>
                  )}

                  {/* Multiple Contexts Info */}
                  {group.vulnerabilities.length > 1 && (
                    <div className="bg-purple-500/10 border border-purple-500/30 rounded-lg p-3 mb-3">
                      <p className="text-sm font-medium text-purple-400 mb-2">
                        This CVE appears in {group.vulnerabilities.length} different contexts:
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {group.uniqueVersions.map((version, idx) => (
                          <span
                            key={idx}
                            className="text-xs bg-bg-card px-2 py-1 rounded border border-purple-500/30"
                          >
                            v{version}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Metadata */}
                  <div className="flex items-center gap-4 text-xs text-text-tertiary">
                    <span>First detected: {formatRelativeTime(group.representative.first_detected)}</span>
                    <span>Last seen: {formatRelativeTime(group.representative.last_seen)}</span>
                    {group.representative.fixed_version && (
                      <span className="text-success">
                        Fix available: v{group.representative.fixed_version}
                      </span>
                    )}
                  </div>

                  {/* Expandable Workloads Section */}
                  {group.totalWorkloads > 0 && (
                    <div className="mt-3 pt-3 border-t">
                      <button
                        onClick={() => toggleExpanded(group.cveId)}
                        className="text-sm text-text-secondary hover:text-text-primary"
                      >
                        {isExpanded ? '▼' : '▶'} Affected workloads count: {group.totalWorkloads}
                      </button>
                      {isExpanded && (
                        <div className="mt-2 space-y-2">
                          {group.vulnerabilities.map((vuln, vIdx) => {
                            const workloads = (vuln as any).workloads || [];
                            if (workloads.length === 0) return null;

                            return (
                              <div key={vIdx} className="bg-bg-secondary rounded p-2">
                                <div className="text-xs font-medium text-text-secondary mb-1">
                                  {vuln.package_name} v{vuln.package_version}
                                </div>
                                <div className="space-y-1">
                                  {workloads.map((workload: any, wIdx: number) => (
                                    <div key={wIdx} className="text-xs text-text-secondary pl-2">
                                      • {workload.namespace}/{workload.name} ({workload.kind})
                                      {workload.image_names && (
                                        <span className="text-text-tertiary ml-1">
                                          - {workload.image_names}
                                        </span>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}