export type SeverityLevel = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';
export type RemediationStatus = 'planned' | 'in_progress' | 'completed' | 'abandoned';
export type RemediationPriority = 'critical' | 'high' | 'medium' | 'low';
export type CompletionMethod = 'auto_resolved' | 'manual' | 'version_upgrade';
export type TriageStatus = 'accepted_risk' | 'false_positive' | 'needs_remediation' | 'duplicate';

export interface Vulnerability {
  id: number;
  cve_id: string;
  package_name: string;
  package_version?: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';
  title?: string;
  description?: string;
  primary_url?: string;
  published_date?: string;
  fixed_version?: string;
  scanner_type: string;
  package_class?: string;
  package_type?: string;
  references?: string[];
  cvss_v3_vector?: string;
  cvss_v3_score?: number;
  first_detected: string;
  last_seen: string;
  affected_workloads?: number;
  workloads?: Workload[];
}

export interface Workload {
  id: number;
  scan_id: number;
  namespace: string;
  name: string;
  kind: 'Deployment' | 'StatefulSet' | 'DaemonSet' | 'Pod' | 'CronJob';
  image_names?: string;
  created_at: string;
  instance_status?: string;
  labels?: Record<string, string>;
}

export interface Comment {
  id: number;
  vulnerability_id: number;
  author: string;
  comment: string;
  comment_type: 'triage' | 'escalation' | 'resolution';
  created_at: string;
}

export interface Escalation {
  id: number;
  vulnerability_id: number;
  escalation_level: number;
  escalated_at: string;
  escalated_by?: string;
  reason?: string;
  status: 'pending' | 'acknowledged' | 'resolved';
  vulnerability?: Vulnerability;
}

export interface Scan {
  id: number;
  namespace: string;
  scan_label: string;
  status: 'running' | 'completed' | 'failed';
  started_at: string;
  completed_at?: string;
  trivy_version?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    total_pages: number;
    has_next: boolean;
    has_prev: boolean;
  };
}

export interface DashboardMetrics {
  total_vulnerabilities: number;
  total_active_vulnerabilities?: number;
  by_severity: {
    CRITICAL: number;
    HIGH: number;
    MEDIUM: number;
    LOW: number;
    UNKNOWN: number;
  };
  by_status: {
    open: number;
    resolved: number;
    false_positive: number;
    accepted_risk: number;
  };
  active_escalations: number;
}

export interface VulnerabilityTrend {
  date: string;
  total: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

export interface VulnerabilityFilters {
  severity?: string[];
  status?: string[];
  namespace?: string;
  scanner_type?: string;
  search?: string;
  workload?: number;
  first_detected_from?: string;
  first_detected_to?: string;
  last_seen_from?: string;
  last_seen_to?: string;
  page?: number;
  limit?: number;
  sort_by?: 'severity' | 'first_detected' | 'last_seen' | 'cve_id';
  sort_order?: 'asc' | 'desc';
}

export interface TriageSession {
  id: number;
  session_date: string;
  status: 'PREPARING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  attendees?: string[];
  notes?: string;
  prep_completed_at?: string;
  session_started_at?: string;
  prep_lead?: string;
  prep_notes?: string;
  vulnerabilities_reviewed: number;
  vulnerabilities_flagged: number;
  bulk_operations_planned: number;
  session_duration_minutes?: number;
  prep_duration_minutes?: number;
  created_at: string;
  completed_at?: string;
  decisions_count?: number;
  decisions?: TriageDecision[];
}

export interface TriageDecision {
  id: number;
  session_id: number;
  vulnerability_id: number;
  decision: 'accept_risk' | 'escalate' | 'remediate' | 'false_positive';
  assigned_to?: string;
  target_date?: string;
  notes?: string;
  created_at: string;
  cve_id?: string;
  package_name?: string;
  package_version?: string;
  severity?: string;
  title?: string;
  description?: string;
  primary_url?: string;
  fixed_version?: string;
  affected_instances?: number;
  affected_workloads?: number;
  remediation_id?: number;
  remediation_status?: 'planned' | 'in_progress' | 'completed' | 'abandoned';
  remediation_priority?: 'critical' | 'high' | 'medium' | 'low';
  remediation_completed_at?: string;
  remediation_completion_method?: 'auto_resolved' | 'manual' | 'version_upgrade';
  remediation_updated_at?: string;
  workloads?: Workload[];
}

export interface TriageReport {
  report_date: string;
  date_range: number;
  namespace: string;
  new_cves: Array<Vulnerability & { affected_workloads: number }>;
  overdue_resolutions: Array<
    Vulnerability & { affected_workloads: number; marked_resolved_at: string }
  >;
  active_escalations: Array<Escalation & { vulnerability: Vulnerability }>;
  summary: {
    new_cves_count: number;
    overdue_resolutions_count: number;
    active_escalations_count: number;
    critical_count: number;
  };
}

// Enhanced Triage System Types
export interface TriagePreparation {
  id?: number;
  session_id: number;
  vulnerability_id: number;
  prep_status: 'pending' | 'reviewed' | 'flagged' | 'bulk_candidate' | 'needs_discussion';
  prep_notes?: string;
  preliminary_decision?: 'accept_risk' | 'escalate' | 'remediate' | 'false_positive';
  priority_flag: 'low' | 'medium' | 'high' | 'critical';
  prep_by: string;
  prep_at: string;
  cve_id?: string;
  package_name?: string;
  severity?: string;
  title?: string;
  workloads?: Array<{
    id: number;
    name: string;
    namespace: string;
    kind: string;
    image_names?: string;
  }>;
}

export interface TriageBulkPlan {
  id?: number;
  session_id: number;
  name: string;
  description?: string;
  filters: {
    severity?: string[];
    namespace?: string[];
    scanner_type?: string[];
    package_name?: string[];
  };
  estimated_count: number;
  actual_count: number;
  status: 'planned' | 'ready' | 'executed' | 'cancelled';
  action: 'accept_risk' | 'escalate' | 'remediate' | 'false_positive';
  metadata?: {
    reason?: string;
    assigned_to?: string;
    target_date?: string;
  };
  created_during_prep: boolean;
  executed_at?: string;
  executed_by?: string;
  created_at: string;
}

export interface TriageBulkOperation {
  id?: number;
  session_id: number;
  bulk_plan_id: number;
  vulnerability_ids: number[];
  operation_type: string;
  metadata?: any;
  executed_by: string;
  executed_at: string;
}

export interface Remediation {
  id?: number;
  vulnerability_id: number;
  triage_decision_id?: number;
  assigned_to?: string;
  target_date?: string;
  priority?: RemediationPriority;
  status: RemediationStatus;
  completed_at?: string;
  completion_method?: CompletionMethod;
  completed_by?: string;
  notes?: string;
  created_at?: string;
  updated_at?: string;
  comments?: RemediationComment[];
}

export interface RemediationComment {
  id?: number;
  remediation_id: number;
  author: string;
  comment: string;
  created_at: string;
}

export interface RemediationWithDetails extends Remediation {
  vulnerability?: Vulnerability;
  triage_decision?: TriageDecision;
  triage_session?: TriageSession;
}

export interface AffectedWorkload {
  id: number;
  name: string;
  namespace: string;
  kind: string;
  image_names?: string;
  instance_id: number;
  instance_status: string;
  labels?: Record<string, string>;
}

export interface TriageSessionMetrics {
  session_id: number;
  total_vulnerabilities: number;
  decisions_made: number;
  bulk_operations_count: number;
  individual_decisions_count: number;
  prep_duration_minutes?: number;
  session_duration_minutes?: number;
  efficiency_score?: number;
  prep_completion_rate?: number;
  decision_breakdown?: {
    accept_risk?: number;
    escalate?: number;
    remediate?: number;
    false_positive?: number;
  };
  calculated_at: string;
}

export interface SbomMetadata {
  id: string;
  image_reference: string;
  format: string;
  first_seen: string;
  last_seen: string;
  package_count: number;
}

export interface SbomHistoryEntry {
  id: string;
  image_reference: string;
  image_digest: string;
  content_hash: string;
  format: string;
  scan_group_id: string;
  first_seen: string;
  last_seen: string;
}

export interface SbomCoverage {
  total_images: number;
  images_with_sbom: number;
  format_breakdown: Record<string, number>;
}

export interface PackageSearchResult {
  name: string;
  version: string;
  type: string;
  purl: string;
  license: string;
  image_reference: string;
}

export interface TopPackageEntry {
  name: string;
  version: string;
  type: string;
  image_count: number;
}

export interface PageResponse<T> {
  content: T[];
  total_elements: number;
  total_pages: number;
  number: number;
  size: number;
}

export interface LicenseDistributionEntry {
  license: string;
  package_count: number;
  image_count: number;
}

export interface SbomDiffResult {
  image_reference: string;
  added: PackageDiff[];
  removed: PackageDiff[];
  changed: PackageChangeDiff[];
  unchanged_count: number;
}

export interface PackageDiff {
  name: string;
  version: string;
  type: string;
  license: string;
}

export interface PackageChangeDiff {
  name: string;
  type: string;
  old_version: string;
  new_version: string;
}

export interface PreScanAlert {
  cve_id: string;
  severity: string;
  package_name: string;
  package_version: string;
  title: string;
  image_reference: string;
  sbom_package_version: string;
}

export interface NamespaceComparison {
  namespace: string;
  total_vulnerabilities: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
  open: number;
  resolved: number;
}
