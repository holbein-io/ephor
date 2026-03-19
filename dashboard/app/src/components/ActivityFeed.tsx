import { useQuery } from '@tanstack/react-query';
import { auditService, AuditLogEntry } from '../services/api/audit.service';
import { formatRelativeTime } from '../utils';

interface ActivityFeedProps {
  entityType?: string;
  entityId?: number;
  limit?: number;
  className?: string;
}

const ACTION_LABELS: Record<string, string> = {
  ESCALATION_CREATED: 'Created escalation',
  ESCALATION_STATUS_CHANGED: 'Updated escalation',
  TRIAGE_SESSION_STARTED: 'Started triage session',
  TRIAGE_SESSION_COMPLETED: 'Completed triage session',
  TRIAGE_DECISION_MADE: 'Made triage decision',
  COMMENT_ADDED: 'Added comment',
  COMMENT_UPDATED: 'Updated comment',
  COMMENT_DELETED: 'Deleted comment',
  VULNERABILITY_STATUS_CHANGED: 'Changed vulnerability status',
  VULNERABILITY_ASSIGNED: 'Assigned vulnerability',
  REMEDIATION_UPDATED: 'Updated remediation',
};

const ACTION_COLORS: Record<string, string> = {
  ESCALATION_CREATED: 'var(--color-severity-critical)',
  VULNERABILITY_STATUS_CHANGED: 'var(--color-severity-high)',
  REMEDIATION_UPDATED: 'var(--color-accent-mint)',
  TRIAGE_SESSION_STARTED: 'var(--color-accent-cool)',
  TRIAGE_SESSION_COMPLETED: 'var(--color-accent-mint)',
  TRIAGE_DECISION_MADE: 'var(--color-accent-cool)',
  COMMENT_ADDED: 'var(--color-text-tertiary)',
};

export function ActivityFeed({ entityType, entityId, limit, className }: ActivityFeedProps) {
  const { data: entries = [], isLoading } = useQuery<AuditLogEntry[]>({
    queryKey: ['activity-feed', entityType, entityId],
    queryFn: async () => {
      if (entityType && entityId) {
        return auditService.getEntityActivity(entityType, entityId);
      }
      return auditService.getAuditLog();
    },
    staleTime: 30000,
    refetchInterval: 60000,
    select: (data) => limit ? data.slice(0, limit) : data,
  });

  if (isLoading) {
    return (
      <div className={`text-sm text-text-tertiary ${className || ''}`}>
        Loading activity...
      </div>
    );
  }

  if (entries.length === 0) {
    return (
      <div className={`text-sm text-text-tertiary ${className || ''}`}>
        No activity recorded yet.
      </div>
    );
  }

  return (
    <div className={`flex flex-col ${className || ''}`}>
      {entries.map((entry, i) => {
        const markerColor = ACTION_COLORS[entry.action] || 'var(--color-text-tertiary)';
        return (
          <div
            key={entry.id}
            className="flex gap-3.5 py-3 border-b border-border-subtle last:border-b-0 animate-fade-up"
            style={{ animationDelay: `${i * 0.05}s` }}
          >
            <div
              className="w-2 h-2 rounded-full mt-[5px] flex-shrink-0"
              style={{ background: markerColor }}
            />
            <div className="flex-1 min-w-0">
              <div className="text-[13px] text-text-primary">
                <span className="font-semibold">{entry.performed_by}</span>{' '}
                <span className="text-text-secondary">
                  {ACTION_LABELS[entry.action] || entry.action.toLowerCase().replace(/_/g, ' ')}
                </span>
                {!entityType && entry.entity_type && (
                  <span className="text-text-tertiary">
                    {' '}on {entry.entity_type.toLowerCase()} #{entry.entity_id}
                  </span>
                )}
              </div>
              <div className="text-[11px] text-text-tertiary mt-0.5">
                {formatRelativeTime(entry.created_at)}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
