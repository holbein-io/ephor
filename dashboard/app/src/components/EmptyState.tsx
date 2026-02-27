import { ReactNode } from 'react';
import { LucideIcon, ShieldOff, Search, FileQuestion, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Button } from './ui/button';

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  secondaryAction?: {
    label: string;
    onClick: () => void;
  };
  children?: ReactNode;
}

export function EmptyState({
  icon: Icon = FileQuestion,
  title,
  description,
  action,
  secondaryAction,
  children
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4">
      <div className="bg-bg-tertiary rounded-full p-4 mb-4">
        <Icon className="h-8 w-8 text-text-tertiary" />
      </div>
      <h3 className="text-lg font-semibold text-text-primary mb-2">{title}</h3>
      <p className="text-sm text-text-tertiary text-center max-w-md mb-6">{description}</p>

      {(action || secondaryAction) && (
        <div className="flex gap-3">
          {action && (
            <Button variant="primary" onClick={action.onClick}>
              {action.label}
            </Button>
          )}
          {secondaryAction && (
            <Button variant="outline" onClick={secondaryAction.onClick}>
              {secondaryAction.label}
            </Button>
          )}
        </div>
      )}

      {children}
    </div>
  );
}

// Pre-configured empty states for common scenarios
export function NoVulnerabilitiesFound({ onClearFilters }: { onClearFilters?: () => void }) {
  return (
    <EmptyState
      icon={Search}
      title="No vulnerabilities found"
      description="No vulnerabilities match your current filters. Try adjusting your search criteria or clearing the filters."
      action={onClearFilters ? {
        label: "Clear all filters",
        onClick: onClearFilters
      } : undefined}
    />
  );
}

export function NoVulnerabilitiesExist() {
  return (
    <EmptyState
      icon={ShieldOff}
      title="No vulnerabilities detected"
      description="Great news! No vulnerabilities have been detected in your scanned workloads. Run a new scan to check for updates."
    />
  );
}

export function NoCommentsYet({ onAddComment }: { onAddComment: () => void }) {
  return (
    <EmptyState
      icon={AlertCircle}
      title="No comments yet"
      description="Add notes about your investigation, findings, or remediation progress to help your team stay informed."
      action={{
        label: "Add first comment",
        onClick: onAddComment
      }}
    />
  );
}

export function NoActiveRemediation() {
  return (
    <EmptyState
      icon={CheckCircle2}
      title="No active remediation"
      description="Remediations are created during triage sessions. Start a triage session to create a remediation plan for this vulnerability."
    />
  );
}

export function NoEscalations() {
  return (
    <EmptyState
      icon={CheckCircle2}
      title="No active escalations"
      description="There are no pending escalations at this time. Escalations are created when vulnerabilities require urgent attention from security leads."
    />
  );
}

export function NoTriageSession() {
  return (
    <EmptyState
      icon={AlertCircle}
      title="No session selected"
      description="Select an existing triage session from the list or create a new one to start reviewing and triaging vulnerabilities."
    />
  );
}

export function NoRecentScans() {
  return (
    <EmptyState
      icon={ShieldOff}
      title="No recent scans"
      description="No scans have been performed recently. Trigger a new scan to start identifying vulnerabilities in your workloads."
    />
  );
}
