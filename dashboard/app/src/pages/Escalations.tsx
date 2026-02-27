import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, Clock, CheckCircle, User, Calendar, MessageSquare } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { escalationService } from '../services/api';
import { formatRelativeTime } from '../utils';

export function Escalations() {
  const [selectedStatus, setSelectedStatus] = useState<string>('all');

  const { data: escalations, isLoading, error, refetch } = useQuery({
    queryKey: ['escalations'],
    queryFn: () => escalationService.getEscalations(),
    refetchInterval: 30000
  });

  const handleStatusUpdate = async (escalationId: number, newStatus: 'acknowledged' | 'resolved') => {
    try {
      await escalationService.updateEscalation(escalationId, { status: newStatus });
      refetch();
    } catch (error) {
      console.error('Failed to update escalation status:', error);
    }
  };

  const filteredEscalations = escalations?.filter(escalation => {
    if (selectedStatus !== 'all' && escalation.status !== selectedStatus) {
      return false;
    }
    return true;
  }) || [];

  const statusCounts = escalations?.reduce((acc, escalation) => {
    acc[escalation.status] = (acc[escalation.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>) || {};

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <AlertTriangle className="mx-auto h-12 w-12 text-danger mb-4" />
          <h3 className="text-lg font-medium text-text-primary mb-2">Failed to load escalations</h3>
          <p className="text-text-tertiary mb-4">There was an error loading the escalations data.</p>
          <Button onClick={() => refetch()}>Try Again</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-display font-bold text-text-primary">Escalations</h1>
          <p className="text-text-secondary">Manage vulnerability escalations requiring attention</p>
        </div>
      </div>

      {/* Status Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <div className="flex-1">
                <p className="text-sm font-medium text-text-secondary">Pending</p>
                <p className="text-2xl font-bold text-warning">{statusCounts.pending || 0}</p>
              </div>
              <Clock className="h-8 w-8 text-warning" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <div className="flex-1">
                <p className="text-sm font-medium text-text-secondary">Acknowledged</p>
                <p className="text-2xl font-bold text-accent">{statusCounts.acknowledged || 0}</p>
              </div>
              <User className="h-8 w-8 text-accent" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <div className="flex-1">
                <p className="text-sm font-medium text-text-secondary">Resolved</p>
                <p className="text-2xl font-bold text-success">{statusCounts.resolved || 0}</p>
              </div>
              <CheckCircle className="h-8 w-8 text-success" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-text-secondary">Status:</label>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-1 border border-border bg-bg-secondary text-text-primary rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-accent/50"
              >
                <option value="all">All</option>
                <option value="pending">Pending</option>
                <option value="acknowledged">Acknowledged</option>
                <option value="resolved">Resolved</option>
              </select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Escalations List */}
      {isLoading ? (
        <div className="space-y-4">
          {[...Array(5)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-20 bg-bg-tertiary rounded"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : filteredEscalations.length === 0 ? (
        <Card>
          <CardContent className="p-8 text-center">
            <AlertTriangle className="mx-auto h-12 w-12 text-text-tertiary mb-4" />
            <h3 className="text-lg font-medium text-text-primary mb-2">No escalations found</h3>
            <p className="text-text-tertiary">
              {escalations?.length === 0
                ? "No escalations have been created yet."
                : "No escalations match the current filters."}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {filteredEscalations.map((escalation) => (
            <Card key={escalation.id} className="hover:border-border-hover transition-colors">
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1 space-y-3">
                    {/* Header Row */}
                    <div className="flex items-center space-x-4">
                      <Badge
                        variant={
                          escalation.vulnerability?.severity === 'CRITICAL' ? 'critical' :
                          escalation.vulnerability?.severity === 'HIGH' ? 'high' :
                          escalation.vulnerability?.severity === 'MEDIUM' ? 'medium' :
                          escalation.vulnerability?.severity === 'LOW' ? 'low' : 'unknown'
                        }
                      >
                        {escalation.vulnerability?.severity}
                      </Badge>

                      <Badge
                        variant={
                          escalation.status === 'resolved' ? 'success' :
                          escalation.status === 'acknowledged' ? 'default' : 'warning'
                        }
                      >
                        {escalation.status}
                      </Badge>
                    </div>

                    {/* Vulnerability Info */}
                    <div className="space-y-2">
                      <div className="flex items-center space-x-2">
                        <span className="font-semibold text-text-primary font-mono">
                          {escalation.vulnerability?.cve_id}
                        </span>
                        <span className="text-text-secondary">
                          {escalation.vulnerability?.package_name}
                        </span>
                      </div>

                      {escalation.vulnerability?.title && (
                        <p className="text-sm text-text-secondary">
                          {escalation.vulnerability.title}
                        </p>
                      )}
                    </div>

                    {/* Escalation Details */}
                    <div className="flex items-center space-x-6 text-sm text-text-tertiary">
                      {escalation.escalated_by && (
                        <div className="flex items-center space-x-1">
                          <User className="h-4 w-4" />
                          <span>Escalated by {escalation.escalated_by}</span>
                        </div>
                      )}

                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                        <span>{formatRelativeTime(escalation.escalated_at)}</span>
                      </div>
                    </div>

                    {/* Reason */}
                    {escalation.reason && (
                      <div className="flex items-start space-x-2">
                        <MessageSquare className="h-4 w-4 text-text-tertiary mt-0.5" />
                        <p className="text-sm text-text-secondary">{escalation.reason}</p>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex flex-col space-y-2">
                    {escalation.status === 'pending' && (
                      <Button
                        size="sm"
                        onClick={() => escalation.id && handleStatusUpdate(escalation.id, 'acknowledged')}
                      >
                        Acknowledge
                      </Button>
                    )}

                    {escalation.status === 'acknowledged' && (
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => escalation.id && handleStatusUpdate(escalation.id, 'resolved')}
                        className="bg-success/15 text-success hover:bg-success/25"
                      >
                        Resolve
                      </Button>
                    )}

                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => window.open(`/vulnerabilities/${escalation.vulnerability_id}`, '_blank')}
                    >
                      View Details
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
