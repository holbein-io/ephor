import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { usersService } from '../services/api/users.service';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { formatRelativeTime } from '../utils';

export function MyItems() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['my-items'],
    queryFn: () => usersService.getMyItems(),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        {[...Array(3)].map((_, i) => (
          <Card key={i} className="animate-pulse">
            <CardContent className="p-6">
              <div className="h-24 bg-bg-tertiary rounded"></div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-text-tertiary">Failed to load your items</p>
      </div>
    );
  }

  const remediations = data?.remediations ?? [];
  const escalations = data?.escalations ?? [];
  const recentComments = data?.recent_comments ?? [];

  return (
    <div className="space-y-6">
      {/* My Remediations */}
      <Card>
        <CardHeader>
          <CardTitle>My Remediations ({remediations.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {remediations.length === 0 ? (
            <p className="text-sm text-text-tertiary text-center py-6">No remediations assigned to you</p>
          ) : (
            <div className="space-y-2">
              {remediations.map((r) => (
                <Link
                  key={r.id}
                  to={`/vulnerabilities/${r.vulnerability_id}`}
                  className="flex items-center justify-between p-3 bg-bg-secondary rounded-lg hover:bg-bg-tertiary transition-colors"
                >
                  <div>
                    <span className="font-mono text-sm font-medium text-text-primary">{r.cve_id}</span>
                    <span className="ml-3 text-xs px-2 py-0.5 rounded-full bg-accent/15 text-accent">{r.status}</span>
                  </div>
                  <span className="text-xs text-text-tertiary">{r.target_date || 'No target date'}</span>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* My Escalations */}
      <Card>
        <CardHeader>
          <CardTitle>My Escalations ({escalations.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {escalations.length === 0 ? (
            <p className="text-sm text-text-tertiary text-center py-6">No escalations created by you</p>
          ) : (
            <div className="space-y-2">
              {escalations.map((e) => (
                <Link
                  key={e.id}
                  to={`/vulnerabilities/${e.vulnerability_id}`}
                  className="flex items-center justify-between p-3 bg-bg-secondary rounded-lg hover:bg-bg-tertiary transition-colors"
                >
                  <div>
                    <span className="font-mono text-sm font-medium text-text-primary">{e.cve_id}</span>
                    <span className="ml-3 text-xs px-2 py-0.5 rounded-full bg-warning/15 text-warning">{e.status}</span>
                  </div>
                  <span className="text-xs text-text-tertiary">{formatRelativeTime(e.escalated_at)}</span>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* My Recent Comments */}
      <Card>
        <CardHeader>
          <CardTitle>My Recent Comments ({recentComments.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {recentComments.length === 0 ? (
            <p className="text-sm text-text-tertiary text-center py-6">No recent comments</p>
          ) : (
            <div className="space-y-2">
              {recentComments.map((c) => {
                const vulnId = c.vulnerability_id || c.entity_id;
                const label = c.cve_id
                  ? c.cve_id
                  : c.entity_type
                    ? `${c.entity_type.toLowerCase()} #${c.entity_id}`
                    : 'comment';

                return (
                  <div key={c.id} className="p-3 bg-bg-secondary rounded-lg">
                    <div className="flex items-center justify-between mb-1">
                      {vulnId ? (
                        <Link
                          to={`/vulnerabilities/${vulnId}`}
                          className="text-xs px-2 py-0.5 rounded-full bg-accent/10 text-accent hover:bg-accent/20 transition-colors"
                        >
                          {label}
                        </Link>
                      ) : (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-bg-tertiary text-text-secondary">
                          {label}
                        </span>
                      )}
                      <span className="text-xs text-text-tertiary">{formatRelativeTime(c.created_at)}</span>
                    </div>
                    <p className="text-sm text-text-secondary line-clamp-2">{c.body}</p>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
