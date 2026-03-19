import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { usersService } from '../services/api/users.service';
import { useUser } from '../contexts/UserContext';
import { formatRelativeTime, formatStatus } from '../utils';

const severityDotColors: Record<string, string> = {
  in_progress: 'bg-severity-critical shadow-[0_0_5px_rgba(232,97,58,0.5)]',
  planned: 'bg-severity-high shadow-[0_0_5px_rgba(232,163,58,0.4)]',
  pending: 'bg-severity-high shadow-[0_0_5px_rgba(232,163,58,0.4)]',
  overdue: 'bg-severity-critical shadow-[0_0_5px_rgba(232,97,58,0.5)]',
  completed: 'bg-severity-medium shadow-[0_0_5px_rgba(201,184,74,0.4)]',
};

const statusBadgeStyles: Record<string, string> = {
  in_progress: 'bg-accent-cool-dim text-accent-cool border-accent-cool/20',
  planned: 'bg-bg-tertiary text-text-secondary border-border',
  pending: 'bg-bg-tertiary text-text-secondary border-border',
  overdue: 'bg-severity-critical/10 text-severity-critical border-severity-critical/20',
  escalated: 'bg-severity-high/10 text-severity-high border-severity-high/20',
  under_review: 'bg-accent-cool-dim text-accent-cool border-accent-cool/20',
  resolved: 'bg-accent-mint-dim text-accent-mint border-accent-mint/20',
  completed: 'bg-accent-mint-dim text-accent-mint border-accent-mint/20',
};

const countBadgeStyles: Record<string, string> = {
  remediations: 'bg-accent-dim text-accent border-accent/25',
  escalations: 'bg-severity-high/10 text-severity-high border-severity-high/25',
  comments: 'bg-accent-cool-dim text-accent-cool border-accent-cool/25',
};

function ItemCard({ title, count, countStyle, children }: {
  title: string;
  count: number;
  countStyle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden animate-fade-up">
      <div className="flex items-center justify-between px-[18px] py-3 border-b border-border-subtle">
        <span className="font-mono text-[10px] font-medium tracking-[0.12em] uppercase text-text-tertiary">
          {title}
        </span>
        <span className={`inline-flex items-center justify-center min-w-[22px] h-[22px] px-[7px] rounded-full font-mono text-[11px] font-medium border ${countStyle}`}>
          {count}
        </span>
      </div>
      {count === 0 ? (
        <div className="text-sm text-text-tertiary text-center py-10">No items</div>
      ) : (
        <div>{children}</div>
      )}
    </div>
  );
}

export function MyItems() {
  const { user } = useUser();
  const { data, isLoading, error } = useQuery({
    queryKey: ['my-items'],
    queryFn: () => usersService.getMyItems(),
  });

  if (isLoading) {
    return (
      <div className="max-w-[1200px] mx-auto space-y-6">
        <div className="h-10 bg-bg-tertiary rounded-2xl animate-pulse" />
        <div className="grid grid-cols-3 gap-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-64 bg-bg-secondary rounded-2xl animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12 text-text-tertiary">Failed to load your items</div>
    );
  }

  const remediations = data?.remediations ?? [];
  const escalations = data?.escalations ?? [];
  const recentComments = data?.recent_comments ?? [];

  const initials = user?.displayName
    ? user.displayName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.username?.slice(0, 2).toUpperCase() || '?';

  return (
    <div className="max-w-[1200px] mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between gap-4 mb-6 animate-fade-up">
        <div>
          <h1 className="font-display text-2xl italic text-text-primary tracking-tight">My Items</h1>
          <p className="text-[13px] text-text-tertiary mt-1">Your assigned work and recent activity</p>
        </div>
        <div className="flex items-center gap-3 px-4 py-2.5 bg-bg-secondary border border-border rounded-2xl">
          <div
            className="w-[38px] h-[38px] rounded-full flex items-center justify-center text-xs font-semibold text-white flex-shrink-0"
            style={{ background: 'linear-gradient(135deg, #E8613A, #C04A2A)' }}
          >
            {initials}
          </div>
          <div>
            <div className="text-[13.5px] font-semibold text-text-primary">{user?.displayName || user?.username}</div>
            <div className="text-[11.5px] text-text-tertiary mt-px">{user?.groups?.[0] || 'Team Member'}</div>
          </div>
        </div>
      </div>

      {/* 3-column grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <ItemCard title="My Remediations" count={remediations.length} countStyle={countBadgeStyles.remediations}>
          {remediations.map(r => (
            <Link
              key={r.id}
              to={`/vulnerabilities/${r.vulnerability_id}`}
              className="flex items-center gap-2.5 px-[18px] py-2.5 border-b border-border-subtle last:border-b-0 hover:bg-bg-hover transition-colors no-underline"
            >
              <span className={`w-[7px] h-[7px] rounded-full flex-shrink-0 ${severityDotColors[r.status] || 'bg-severity-medium'}`} />
              <span className="font-mono text-xs font-medium text-accent-cool flex-1 min-w-0 truncate">{r.cve_id}</span>
              <div className="flex items-center gap-2 flex-shrink-0">
                <span className={`text-[10.5px] font-medium px-2 py-0.5 rounded border ${statusBadgeStyles[r.status] || statusBadgeStyles.pending}`}>
                  {formatStatus(r.status)}
                </span>
                <span className="font-mono text-[10.5px] text-text-tertiary whitespace-nowrap">{r.target_date || '-'}</span>
              </div>
            </Link>
          ))}
        </ItemCard>

        <ItemCard title="My Escalations" count={escalations.length} countStyle={countBadgeStyles.escalations}>
          {escalations.map(e => (
            <div key={e.id} className="px-[18px] py-3 border-b border-border-subtle last:border-b-0 hover:bg-bg-hover transition-colors">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Link to={`/vulnerabilities/${e.vulnerability_id}`} className="font-mono text-xs font-medium text-accent-cool no-underline hover:underline">
                    {e.cve_id}
                  </Link>
                  <span className={`text-[10.5px] font-medium px-2 py-0.5 rounded border ${statusBadgeStyles[e.status] || statusBadgeStyles.escalated}`}>
                    {formatStatus(e.status)}
                  </span>
                </div>
                <span className="text-[11px] text-text-tertiary whitespace-nowrap">{formatRelativeTime(e.escalated_at)}</span>
              </div>
            </div>
          ))}
        </ItemCard>

        <ItemCard title="Recent Comments" count={recentComments.length} countStyle={countBadgeStyles.comments}>
          {recentComments.map(c => {
            const vulnId = c.vulnerability_id || c.entity_id;
            const label = c.cve_id || (c.entity_type ? `${c.entity_type.toLowerCase()} #${c.entity_id}` : 'comment');
            return (
              <div key={c.id} className="px-[18px] py-3 border-b border-border-subtle last:border-b-0 hover:bg-bg-hover transition-colors">
                <div className="flex items-center justify-between gap-2 mb-1">
                  {vulnId ? (
                    <Link to={`/vulnerabilities/${vulnId}`} className="font-mono text-xs font-medium text-accent-cool no-underline hover:underline">{label}</Link>
                  ) : (
                    <span className="font-mono text-xs text-text-secondary">{label}</span>
                  )}
                  <span className="text-[11px] text-text-tertiary whitespace-nowrap">{formatRelativeTime(c.created_at)}</span>
                </div>
                <p className="text-[12.5px] text-text-secondary leading-[1.45] line-clamp-2">{c.body}</p>
              </div>
            );
          })}
        </ItemCard>
      </div>
    </div>
  );
}
