import { useState, useEffect, useMemo } from 'react';
import { CheckCircle, X, AlertTriangle, Shield, MessageSquare, User, Calendar, ExternalLink, FileText } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { TriagePreparation } from '../../types';
import { SEVERITY_COLORS } from '../../constants/colors';
import { useTriageDecisions } from '../../hooks/useTriageData';
import { AffectedWorkloads } from './AffectedWorkloads';

interface TriageDecisionMakerProps {
  preparations: TriagePreparation[] | undefined;
  isLoading: boolean;
  sessionId: number;
  onMakeDecision: (
    vulnerabilityId: number,
    decision: {
      status: 'accepted_risk' | 'false_positive' | 'needs_remediation' | 'duplicate';
      notes?: string;
      assigned_to?: string;
      target_date?: string;
      priority?: string;
    }
  ) => void;
}

export function TriageDecisionMaker({
  preparations,
  isLoading,
  sessionId,
  onMakeDecision
}: TriageDecisionMakerProps) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [decisionNotes, setDecisionNotes] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [priority, setPriority] = useState<string>('medium');
  const [decidedVulns, setDecidedVulns] = useState<Set<number>>(new Set());
  const [applyToAll, setApplyToAll] = useState(true);
  const [decidedWorkloadsCount, setDecidedWorkloadsCount] = useState(0);

  // Fetch existing decisions to initialize decidedVulns
  const { data: existingDecisions } = useTriageDecisions(sessionId);

  // Calculate total workloads count across all preparations
  const totalWorkloadsCount = useMemo(() => {
    if (!preparations) return 0;
    return preparations.reduce((total, prep) => {
      const workloads = (prep as any).affected_workloads || [];
      return total + Math.max(workloads.length, 1); // At least 1 if no workloads data
    }, 0);
  }, [preparations]);

  // Initialize decidedVulns and count from existing decisions
  useEffect(() => {
    if (existingDecisions && existingDecisions.length > 0) {
      const decidedIds = new Set(existingDecisions.map((d: any) => d.vulnerability_id));
      setDecidedVulns(decidedIds);

      // Count workloads for already decided vulnerabilities
      if (preparations) {
        let count = 0;
        preparations.forEach(prep => {
          if (decidedIds.has(prep.vulnerability_id)) {
            const workloads = (prep as any).affected_workloads || [];
            count += Math.max(workloads.length, 1);
          }
        });
        setDecidedWorkloadsCount(count);
      }
    }
  }, [existingDecisions, preparations]);

  const pendingPreparations = preparations?.filter(p => !decidedVulns.has(p.vulnerability_id)) || [];
  const currentPrep = pendingPreparations[currentIndex];

  const handleDecision = (status: 'accepted_risk' | 'false_positive' | 'needs_remediation' | 'duplicate') => {
    if (!currentPrep) return;

    onMakeDecision(currentPrep.vulnerability_id, {
      status,
      notes: decisionNotes,
      assigned_to: status === 'needs_remediation' ? assignedTo : undefined,
      target_date: status === 'needs_remediation' ? targetDate : undefined,
      priority: status === 'needs_remediation' ? priority : undefined
    });

    // Mark as decided and update workload count
    setDecidedVulns(prev => new Set(prev).add(currentPrep.vulnerability_id));

    // Count workloads for this decision
    const workloads = (currentPrep as any).affected_workloads || [];
    const workloadsForThisDecision = Math.max(workloads.length, 1);
    setDecidedWorkloadsCount(prev => prev + workloadsForThisDecision);

    // Reset form
    setDecisionNotes('');
    setAssignedTo('');
    setTargetDate('');
    setPriority('medium');
    setApplyToAll(true);

    // Keep index at 0 since the array will shrink after filtering
    // The next pending item will automatically appear at index 0
    setCurrentIndex(0);
  };

  const getSeverityColor = (severity: string) => {
    return SEVERITY_COLORS[severity as keyof typeof SEVERITY_COLORS] || SEVERITY_COLORS.UNKNOWN;
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <div className="text-text-tertiary">Loading preparations...</div>
        </CardContent>
      </Card>
    );
  }

  if (!preparations || preparations.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <AlertTriangle className="h-12 w-12 text-warning mb-3" />
          <h3 className="text-lg font-medium text-text-primary">No Vulnerabilities Prepared</h3>
          <p className="mt-2 text-sm text-text-tertiary">
            Add vulnerabilities to this session during the preparation phase first.
          </p>
        </CardContent>
      </Card>
    );
  }

  if (pendingPreparations.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <CheckCircle className="h-12 w-12 text-success mb-3" />
          <h3 className="text-lg font-medium text-text-primary">All Decisions Complete!</h3>
          <p className="mt-2 text-sm text-text-tertiary">
            You've made decisions for all {totalWorkloadsCount} workload instances across {preparations.length} vulnerabilities.
          </p>
        </CardContent>
      </Card>
    );
  }

  // Get current prep's workload count for display
  const currentWorkloads = (currentPrep as any)?.affected_workloads || [];
  const currentWorkloadCount = currentWorkloads.length;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Shield className="w-5 h-5" />
            Triage Decision
          </CardTitle>
          <div className="flex items-center gap-3 text-sm">
            <div className="flex items-center gap-1 text-text-secondary">
              <span className="font-medium">{decidedWorkloadsCount}</span>
              <span>/</span>
              <span>{totalWorkloadsCount}</span>
              <span className="text-text-tertiary">instances</span>
            </div>
            {currentWorkloadCount > 1 && (
              <Badge variant="info" className="text-xs">
                +{currentWorkloadCount} workloads
              </Badge>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {currentPrep && (
          <>
            {/* Vulnerability Details */}
            <div className="border rounded-lg p-4 space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <Badge
                    variant="info"
                    className={`text-xs ${getSeverityColor((currentPrep as any).severity).tailwind}`}
                  >
                    {(currentPrep as any).severity}
                  </Badge>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="font-mono font-medium text-text-primary">
                        {(currentPrep as any).cve_id}
                      </h4>
                    </div>
                    <p className="text-sm text-text-secondary mt-1">
                      {(currentPrep as any).package_name}
                      {(currentPrep as any).package_version && ` v${(currentPrep as any).package_version}`}
                    </p>
                  </div>
                </div>
                {(currentPrep as any).priority_flag && (
                  <Badge variant="warning" className="text-xs">
                    Priority: {(currentPrep as any).priority_flag}
                  </Badge>
                )}
              </div>

              {/* Portal Links */}
              <div className="flex flex-wrap gap-2 pt-2 border-t">
                <span className="text-xs font-medium text-text-secondary self-center">Links:</span>
                <a
                  href={`/vulnerabilities/${currentPrep.vulnerability_id}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-indigo-500/10 text-indigo-400 hover:bg-indigo-500/15 rounded border border-indigo-500/30 font-medium"
                >
                  <FileText className="w-3 h-3" />
                  View Details
                </a>
                {(currentPrep as any).primary_url && (
                  <a
                    href={(currentPrep as any).primary_url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-accent/10 text-accent hover:bg-accent/15 rounded border border-accent/30"
                  >
                    <ExternalLink className="w-3 h-3" />
                    Advisory
                  </a>
                )}
                <a
                  href={`https://nvd.nist.gov/vuln/detail/${(currentPrep as any).cve_id}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-purple-500/10 text-purple-400 hover:bg-purple-500/15 rounded border border-purple-500/30"
                >
                  <ExternalLink className="w-3 h-3" />
                  NVD
                </a>
                <a
                  href={`https://www.cisa.gov/known-exploited-vulnerabilities-catalog?search_api_fulltext=${(currentPrep as any).cve_id}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-danger/10 text-danger hover:bg-danger/10 rounded border border-danger/30"
                >
                  <ExternalLink className="w-3 h-3" />
                  CISA KEV
                </a>
                <a
                  href={`https://cve.mitre.org/cgi-bin/cvename.cgi?name=${(currentPrep as any).cve_id}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-bg-secondary text-text-secondary hover:bg-bg-tertiary rounded border border-border"
                >
                  <ExternalLink className="w-3 h-3" />
                  MITRE
                </a>
              </div>

              {(currentPrep as any).title && (
                <p className="text-sm text-text-secondary">{(currentPrep as any).title}</p>
              )}

              {(currentPrep as any).description && (
                <p className="text-xs text-text-tertiary">{(currentPrep as any).description}</p>
              )}

              {(currentPrep as any).fixed_version && (
                <p className="text-xs text-success">
                  Fix available: v{(currentPrep as any).fixed_version}
                </p>
              )}

              {(currentPrep as any).prep_notes && (
                <div className="bg-accent/10 border border-accent/30 rounded p-2">
                  <p className="text-xs font-medium text-accent-hover mb-1">Preparation Notes:</p>
                  <p className="text-xs text-accent">{(currentPrep as any).prep_notes}</p>
                </div>
              )}
            </div>

            {/* Affected Workloads */}
            <AffectedWorkloads
              workloads={currentWorkloads}
              packageName={(currentPrep as any).package_name || ''}
              packageVersion={(currentPrep as any).package_version}
              fixedVersion={(currentPrep as any).fixed_version}
              applyToAll={applyToAll}
              onApplyToAllChange={setApplyToAll}
            />

            {/* Decision Form */}
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  <MessageSquare className="w-4 h-4 inline mr-1" />
                  Decision Notes
                </label>
                <textarea
                  value={decisionNotes}
                  onChange={(e) => setDecisionNotes(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-md text-sm"
                  rows={3}
                  placeholder="Add any notes about this decision..."
                />
              </div>

              {/* Show additional fields for remediation */}
              <div className="space-y-3 p-3 bg-bg-secondary rounded-lg">
                <p className="text-xs font-medium text-text-secondary">If marking for remediation:</p>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">
                      <User className="w-3 h-3 inline mr-1" />
                      Assign To
                    </label>
                    <input
                      type="text"
                      value={assignedTo}
                      onChange={(e) => setAssignedTo(e.target.value)}
                      className="w-full px-2 py-1 border border-border rounded text-sm"
                      placeholder="Username"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">
                      <Calendar className="w-3 h-3 inline mr-1" />
                      Target Date
                    </label>
                    <input
                      type="date"
                      value={targetDate}
                      onChange={(e) => setTargetDate(e.target.value)}
                      className="w-full px-2 py-1 border border-border rounded text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">Priority</label>
                    <select
                      value={priority}
                      onChange={(e) => setPriority(e.target.value)}
                      className="w-full px-2 py-1 border border-border rounded text-sm"
                    >
                      <option value="low">Low</option>
                      <option value="medium">Medium</option>
                      <option value="high">High</option>
                      <option value="critical">Critical</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Decision Buttons */}
              <div className="grid grid-cols-2 gap-2 pt-2">
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => handleDecision('needs_remediation')}
                  className="bg-success hover:bg-success/80 border-success"
                >
                  <CheckCircle className="w-4 h-4 mr-1" />
                  Needs Remediation
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => handleDecision('accepted_risk')}
                  className="bg-warning hover:bg-warning/80 text-white"
                >
                  <AlertTriangle className="w-4 h-4 mr-1" />
                  Accept Risk
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => handleDecision('false_positive')}
                >
                  <X className="w-4 h-4 mr-1" />
                  False Positive
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => handleDecision('duplicate')}
                >
                  Duplicate
                </Button>
              </div>

              {/* Navigation */}
              {pendingPreparations.length > 1 && (
                <div className="flex items-center justify-between pt-2 border-t">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setCurrentIndex(Math.max(0, currentIndex - 1))}
                    disabled={currentIndex === 0}
                  >
                    Previous
                  </Button>
                  <span className="text-sm text-text-secondary">
                    {currentIndex + 1} of {pendingPreparations.length} pending
                  </span>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setCurrentIndex(Math.min(pendingPreparations.length - 1, currentIndex + 1))}
                    disabled={currentIndex === pendingPreparations.length - 1}
                  >
                    Skip
                  </Button>
                </div>
              )}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}