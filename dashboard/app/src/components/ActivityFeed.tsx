import { useEffect, useState } from 'react';
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

export function ActivityFeed({ entityType, entityId, limit, className }: ActivityFeedProps) {
  const [entries, setEntries] = useState<AuditLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchActivity() {
      try {
        let data: AuditLogEntry[];
        if (entityType && entityId) {
          data = await auditService.getEntityActivity(entityType, entityId);
        } else {
          data = await auditService.getAuditLog();
        }
        setEntries(limit ? data.slice(0, limit) : data);
      } catch (error) {
        console.error('Failed to load activity feed:', error);
      } finally {
        setIsLoading(false);
      }
    }

    fetchActivity();
  }, [entityType, entityId, limit]);

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
    <div className={`space-y-2 ${className || ''}`}>
      {entries.map((entry) => (
        <div
          key={entry.id}
          className="flex items-start gap-2 text-sm border-l-2 border-border pl-3 py-1"
        >
          <div className="flex-1 min-w-0">
            <span className="font-medium text-text-primary">{entry.performed_by}</span>{' '}
            <span className="text-text-secondary">
              {ACTION_LABELS[entry.action] || entry.action.toLowerCase().replace(/_/g, ' ')}
            </span>
            {!entityType && (
              <span className="text-text-tertiary">
                {' '}on {entry.entity_type?.toLowerCase()} #{entry.entity_id}
              </span>
            )}
          </div>
          <span className="text-xs text-text-tertiary whitespace-nowrap">
            {formatRelativeTime(entry.created_at)}
          </span>
        </div>
      ))}
    </div>
  );
}
