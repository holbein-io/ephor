import { Link } from 'react-router-dom';
import { AlertTriangle, Shield, MessageSquare, User, Calendar, FileText, ExternalLink, ClipboardList } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { SEVERITY_COLORS } from '../../constants/colors';
import { formatDate } from '../../utils';

interface TriageDecision {
  id: number;
  session_id: number;
  vulnerability_id: number;
  status: 'accepted_risk' | 'false_positive' | 'needs_remediation' | 'duplicate';
  notes?: string;
  assigned_to?: string;
  target_date?: string;
  priority?: string;
  created_at: string;
  updated_at: string;
  // Joined data from vulnerability
  cve_id?: string;
  package_name?: string;
  package_version?: string;
  severity?: string;
  title?: string;
  fixed_version?: string;
  // Joined data from remediation
  remediation_id?: number;
  remediation_status?: string;
  remediation_priority?: string;
  remediation_target_date?: string;
  remediation_assigned_to?: string;
}

interface TriageSessionReviewProps {
  decisions: TriageDecision[] | undefined;
  preparations: any[] | undefined;
  isLoadingDecisions: boolean;
  isLoadingPreparations: boolean;
}

export function TriageSessionReview({
  decisions,
  preparations,
  isLoadingDecisions,
  isLoadingPreparations
}: TriageSessionReviewProps) {
  const getSeverityColor = (severity: string) => {
    return SEVERITY_COLORS[severity as keyof typeof SEVERITY_COLORS] || SEVERITY_COLORS.UNKNOWN;
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'accepted_risk':
        return 'Accepted Risk';
      case 'false_positive':
        return 'False Positive';
      case 'needs_remediation':
        return 'Needs Remediation';
      case 'duplicate':
        return 'Duplicate';
      default:
        return status;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'accepted_risk':
        return 'bg-warning/15 text-warning border-warning/30';
      case 'false_positive':
        return 'bg-bg-tertiary text-text-primary border-border';
      case 'needs_remediation':
        return 'bg-success/15 text-success border-success/30';
      case 'duplicate':
        return 'bg-purple-500/15 text-purple-400 border-purple-500/30';
      default:
        return 'bg-bg-tertiary text-text-primary border-border';
    }
  };

  if (isLoadingDecisions || isLoadingPreparations) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <div className="text-text-tertiary">Loading session details...</div>
        </CardContent>
      </Card>
    );
  }

  const totalDecisions = decisions?.length || 0;
  const remediationCount = decisions?.filter(d => d.status === 'needs_remediation').length || 0;
  const acceptedRiskCount = decisions?.filter(d => d.status === 'accepted_risk').length || 0;
  const falsePositiveCount = decisions?.filter(d => d.status === 'false_positive').length || 0;
  const duplicateCount = decisions?.filter(d => d.status === 'duplicate').length || 0;

  // Group decisions by status for organized display
  const groupedDecisions = {
    needs_remediation: decisions?.filter(d => d.status === 'needs_remediation') || [],
    accepted_risk: decisions?.filter(d => d.status === 'accepted_risk') || [],
    false_positive: decisions?.filter(d => d.status === 'false_positive') || [],
    duplicate: decisions?.filter(d => d.status === 'duplicate') || []
  };

  return (
    <div className="space-y-6">
      {/* Summary Stats */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="w-5 h-5" />
            Session Review Summary
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-5 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-text-primary">{totalDecisions}</div>
              <div className="text-sm text-text-tertiary">Total Decisions</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-success">{remediationCount}</div>
              <div className="text-sm text-text-tertiary">Need Remediation</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-warning">{acceptedRiskCount}</div>
              <div className="text-sm text-text-tertiary">Accepted Risk</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-text-secondary">{falsePositiveCount}</div>
              <div className="text-sm text-text-tertiary">False Positives</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-400">{duplicateCount}</div>
              <div className="text-sm text-text-tertiary">Duplicates</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Decisions by Status */}
      {Object.entries(groupedDecisions).map(([status, statusDecisions]) => {
        if (statusDecisions.length === 0) return null;

        return (
          <Card key={status}>
            <CardHeader>
              <CardTitle className="text-lg">
                <Badge variant="info" className={`text-xs ${getStatusColor(status)}`}>
                  {getStatusLabel(status)} ({statusDecisions.length})
                </Badge>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {statusDecisions.map((decision) => {
                  // Find matching preparation for additional context
                  const preparation = preparations?.find(p => p.vulnerability_id === decision.vulnerability_id);

                  return (
                    <div key={decision.id} className="border rounded-lg p-4 space-y-3">
                      {/* Vulnerability Info */}
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-3">
                          <Badge
                            variant="info"
                            className={`text-xs ${getSeverityColor(decision.severity || 'UNKNOWN').tailwind}`}
                          >
                            {decision.severity}
                          </Badge>
                          <div>
                            <div className="flex items-center gap-2">
                              <Link
                                to={`/vulnerabilities/${decision.vulnerability_id}`}
                                className="font-mono font-medium text-accent hover:text-accent-hover hover:underline flex items-center gap-1"
                              >
                                {decision.cve_id || 'Unknown CVE'}
                                <ExternalLink className="w-3 h-3" />
                              </Link>
                            </div>
                            <p className="text-sm text-text-secondary mt-1">
                              {decision.package_name}
                              {decision.package_version && ` v${decision.package_version}`}
                              {decision.fixed_version && (
                                <span className="text-success ml-2">
                                  (Fix: v{decision.fixed_version})
                                </span>
                              )}
                            </p>
                          </div>
                        </div>
                        <div className="text-xs text-text-tertiary">
                          {formatDate(decision.created_at)}
                        </div>
                      </div>

                      {decision.title && (
                        <p className="text-sm text-text-secondary">{decision.title}</p>
                      )}

                      {/* Decision Details */}
                      {decision.status === 'needs_remediation' && (
                        <div className="bg-success/10 border border-success/30 rounded p-3 space-y-2">
                          <div className="flex items-center gap-2 mb-2">
                            <ClipboardList className="w-4 h-4 text-success" />
                            <span className="text-sm font-medium text-success">Remediation Plan</span>
                            {decision.remediation_status && (
                              <Badge
                                variant="info"
                                className={`text-xs ml-auto ${
                                  decision.remediation_status === 'completed'
                                    ? 'bg-success/20 text-success'
                                    : decision.remediation_status === 'in_progress'
                                    ? 'bg-warning/20 text-text-primary'
                                    : decision.remediation_status === 'abandoned'
                                    ? 'bg-bg-tertiary text-text-secondary'
                                    : 'bg-accent/20 text-accent'
                                }`}
                              >
                                {decision.remediation_status === 'in_progress' ? 'In Progress' :
                                 decision.remediation_status === 'completed' ? 'Completed' :
                                 decision.remediation_status === 'abandoned' ? 'Abandoned' : 'Planned'}
                              </Badge>
                            )}
                          </div>
                          <div className="grid grid-cols-3 gap-3 text-sm">
                            {(decision.remediation_assigned_to || decision.assigned_to) && (
                              <div className="flex items-center gap-1">
                                <User className="w-3 h-3 text-success" />
                                <span className="text-text-secondary">
                                  Assigned: {decision.remediation_assigned_to || decision.assigned_to}
                                </span>
                              </div>
                            )}
                            {(decision.remediation_target_date || decision.target_date) && (
                              <div className="flex items-center gap-1">
                                <Calendar className="w-3 h-3 text-success" />
                                <span className="text-text-secondary">
                                  Target: {formatDate((decision.remediation_target_date || decision.target_date)!)}
                                </span>
                              </div>
                            )}
                            {(decision.remediation_priority || decision.priority) && (
                              <div>
                                <Badge variant="info" className="text-xs capitalize">
                                  {decision.remediation_priority || decision.priority}
                                </Badge>
                              </div>
                            )}
                          </div>
                          {decision.remediation_id && (
                            <div className="mt-2 pt-2 border-t border-success/30">
                              <Link
                                to={`/vulnerabilities/${decision.vulnerability_id}`}
                                className="text-xs text-success hover:text-success/80 hover:underline flex items-center gap-1"
                              >
                                View Remediation Details
                                <ExternalLink className="w-3 h-3" />
                              </Link>
                            </div>
                          )}
                        </div>
                      )}

                      {/* Notes */}
                      {decision.notes && (
                        <div className="bg-accent/10 border border-accent/30 rounded p-3">
                          <div className="flex items-start gap-2">
                            <MessageSquare className="w-4 h-4 text-accent mt-0.5" />
                            <div>
                              <p className="text-xs font-medium text-accent-hover mb-1">Decision Notes:</p>
                              <p className="text-sm text-accent">{decision.notes}</p>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Preparation Notes if available */}
                      {preparation?.prep_notes && (
                        <div className="bg-bg-secondary border border-border rounded p-3">
                          <div className="flex items-start gap-2">
                            <FileText className="w-4 h-4 text-text-secondary mt-0.5" />
                            <div>
                              <p className="text-xs font-medium text-text-secondary mb-1">Preparation Notes:</p>
                              <p className="text-sm text-text-secondary">{preparation.prep_notes}</p>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </CardContent>
          </Card>
        );
      })}

      {/* No Decisions Message */}
      {(!decisions || decisions.length === 0) && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <AlertTriangle className="h-12 w-12 text-warning mb-3" />
            <h3 className="text-lg font-medium text-text-primary">No Decisions Recorded</h3>
            <p className="mt-2 text-sm text-text-tertiary">
              No decisions were made during this triage session.
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}