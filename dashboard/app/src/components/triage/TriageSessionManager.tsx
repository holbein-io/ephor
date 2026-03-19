import { Plus } from 'lucide-react';
import { TriageSession } from '../../types';
import { formatDateOnly } from '../../utils';
import { cn } from '../../utils';

interface TriageSessionManagerProps {
  sessions: TriageSession[] | undefined;
  currentSession: TriageSession | null;
  isLoading: boolean;
  onSelectSession: (session: TriageSession) => void;
  onCreateSession: () => void;
}

const statusBadgeStyles: Record<string, string> = {
  PREPARING: 'bg-accent-dim border-accent/25 text-accent',
  ACTIVE: 'bg-accent-dim border-accent/25 text-accent',
  COMPLETED: 'bg-accent-mint-dim border-accent-mint/20 text-accent-mint',
  CANCELLED: 'bg-bg-hover border-border text-text-tertiary',
};

const statusLabels: Record<string, string> = {
  PREPARING: 'Preparing',
  ACTIVE: 'Active',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

export function TriageSessionManager({
  sessions,
  currentSession,
  isLoading,
  onSelectSession,
  onCreateSession
}: TriageSessionManagerProps) {
  return (
    <aside className="w-[280px] flex-shrink-0 bg-bg-tertiary border-r border-border-subtle flex flex-col overflow-y-auto">
      <div className="flex items-center justify-between px-4 py-4 border-b border-border-subtle">
        <span className="font-mono text-[10px] font-medium tracking-[0.08em] uppercase text-text-tertiary">
          Triage Sessions
        </span>
        <button
          onClick={onCreateSession}
          className="flex items-center gap-1 px-2.5 py-1.5 bg-accent-dim border border-accent/25 rounded-lg text-accent text-xs font-medium hover:bg-accent/20 transition-colors"
        >
          <Plus className="w-3.5 h-3.5" />
          New Session
        </button>
      </div>

      <div className="p-2 flex flex-col gap-1">
        {isLoading ? (
          <div className="text-center py-8 text-text-tertiary text-sm">Loading sessions...</div>
        ) : !sessions || sessions.length === 0 ? (
          <div className="text-center py-8 text-text-tertiary text-sm">
            No sessions yet.
          </div>
        ) : (
          sessions.map((session, i) => {
            const isSelected = currentSession?.id === session.id;
            const badgeStyle = statusBadgeStyles[session.status] || statusBadgeStyles.PREPARING;
            const label = statusLabels[session.status] || session.status;

            return (
              <div
                key={session.id}
                onClick={() => onSelectSession(session)}
                className={cn(
                  'px-3 py-3 rounded-lg cursor-pointer border-l-[3px] border-transparent transition-colors animate-fade-up',
                  isSelected
                    ? 'bg-accent-dim border-l-accent border-accent/[0.18]'
                    : 'hover:bg-bg-hover'
                )}
                style={{ animationDelay: `${i * 0.05}s` }}
              >
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[13px] font-medium text-text-primary">
                    {formatDateOnly(session.session_date)}
                  </span>
                  <span className={cn('text-[10px] font-semibold tracking-wider uppercase px-[7px] py-0.5 rounded-full border', badgeStyle)}>
                    {label}
                  </span>
                </div>
                <div className="text-[11px] text-text-secondary font-mono">
                  {session.vulnerabilities_reviewed || 0} reviewed / {session.decisions_count || 0} decided
                </div>
              </div>
            );
          })
        )}
      </div>
    </aside>
  );
}
