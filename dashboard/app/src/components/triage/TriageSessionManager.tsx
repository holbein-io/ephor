import { Plus, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { TriageSession } from '../../types';
import { formatDateOnly } from '../../utils';
import { TRIAGE_SESSION_STATUS_COLORS } from '../../constants/colors';

interface TriageSessionManagerProps {
  sessions: TriageSession[] | undefined;
  currentSession: TriageSession | null;
  isLoading: boolean;
  hasVulnerabilities: boolean;
  onSelectSession: (session: TriageSession) => void;
  onCreateSession: () => void;
}

export function TriageSessionManager({
  sessions,
  currentSession,
  isLoading,
  hasVulnerabilities,
  onSelectSession,
  onCreateSession
}: TriageSessionManagerProps) {
  const getStatusColor = (status: string) => {
    return TRIAGE_SESSION_STATUS_COLORS[status as keyof typeof TRIAGE_SESSION_STATUS_COLORS] ||
           TRIAGE_SESSION_STATUS_COLORS.PREPARING;
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <div className="text-text-tertiary">Loading sessions...</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Calendar className="w-5 h-5" />
            Triage Sessions
          </CardTitle>
          <Button
            onClick={onCreateSession}
            size="sm"
            disabled={!hasVulnerabilities}
            title={!hasVulnerabilities ? 'No vulnerabilities available to triage' : undefined}
          >
            <Plus className="w-4 h-4 mr-2" />
            New Session
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {!hasVulnerabilities && (
          <div className="text-center py-4 px-3 mb-3 rounded-md bg-warning/10 text-warning text-sm">
            No open vulnerabilities found. Scan results are required before creating a triage session.
          </div>
        )}
        {!sessions || sessions.length === 0 ? (
          <div className="text-center py-8 text-text-tertiary">
            <p>No triage sessions found.</p>
            {hasVulnerabilities && (
              <p className="text-sm mt-2">Create your first session to get started.</p>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {sessions.map((session) => {
              const isActive = currentSession?.id === session.id;
              const statusConfig = getStatusColor(session.status);

              return (
                <div
                  key={session.id}
                  className={`border rounded-lg p-3 cursor-pointer transition-all ${
                    isActive
                      ? 'border-accent bg-accent/10'
                      : 'border-border hover:border-border hover:shadow-sm'
                  }`}
                  onClick={() => onSelectSession(session)}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <h3 className="font-medium text-text-primary">
                        {formatDateOnly(session.session_date)}
                      </h3>
                      <Badge
                        variant="info"
                        className={`text-xs ${statusConfig.tailwind}`}
                      >
                        {statusConfig.label}
                      </Badge>
                    </div>

                    {isActive && (
                      <Badge variant="default" className="bg-accent">
                        Active
                      </Badge>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}