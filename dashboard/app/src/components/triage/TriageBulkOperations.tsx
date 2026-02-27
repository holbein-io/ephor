import { useState } from 'react';
import { Play, Plus, Trash2, ChevronDown, ChevronUp, Target } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { TriageBulkPlan } from '../../types';
import { formatDate } from '../../utils';

interface TriageBulkOperationsProps {
  bulkPlans: TriageBulkPlan[] | undefined;
  isLoading: boolean;
  sessionId: number;
  onCreatePlan: (plan: {
    name: string;
    description: string;
    filters: any;
    action: string;
    metadata?: any;
  }) => void;
  onExecutePlan: (planId: number) => void;
  onDeletePlan?: (planId: number) => void;
}

export function TriageBulkOperations({
  bulkPlans,
  isLoading,
  onCreatePlan,
  onExecutePlan,
  onDeletePlan
}: TriageBulkOperationsProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [expandedPlans, setExpandedPlans] = useState<Set<number>>(new Set());
  const [newPlan, setNewPlan] = useState({
    name: '',
    description: '',
    action: 'accept_risk',
    filters: {
      severity: [] as string[],
      namespace: '',
      package_pattern: ''
    }
  });

  const togglePlanExpanded = (planId: number) => {
    setExpandedPlans(prev => {
      const newSet = new Set(prev);
      if (newSet.has(planId)) {
        newSet.delete(planId);
      } else {
        newSet.add(planId);
      }
      return newSet;
    });
  };

  const handleCreatePlan = () => {
    if (newPlan.name) {
      onCreatePlan({
        name: newPlan.name,
        description: newPlan.description,
        filters: newPlan.filters,
        action: newPlan.action
      });
      setNewPlan({
        name: '',
        description: '',
        action: 'accept_risk',
        filters: {
          severity: [],
          namespace: '',
          package_pattern: ''
        }
      });
      setShowCreateForm(false);
    }
  };

  const getActionColor = (action: string): string => {
    switch (action) {
      case 'accept_risk':
        return 'text-warning bg-warning/10';
      case 'false_positive':
        return 'text-text-secondary bg-bg-secondary';
      case 'needs_remediation':
        return 'text-danger bg-danger/10';
      case 'escalate':
        return 'text-severity-high bg-severity-high/10';
      default:
        return 'text-accent bg-accent/10';
    }
  };

  const getActionLabel = (action: string): string => {
    switch (action) {
      case 'accept_risk':
        return 'Accept Risk';
      case 'false_positive':
        return 'Mark as False Positive';
      case 'needs_remediation':
        return 'Needs Remediation';
      case 'escalate':
        return 'Escalate';
      default:
        return action;
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <div className="text-text-tertiary">Loading bulk operations...</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Target className="w-5 h-5" />
            Bulk Operations
          </CardTitle>
          <Button
            size="sm"
            onClick={() => setShowCreateForm(!showCreateForm)}
          >
            <Plus className="w-4 h-4 mr-2" />
            New Bulk Plan
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {/* Create Form */}
        {showCreateForm && (
          <div className="border rounded-lg p-4 mb-4 bg-bg-secondary">
            <h3 className="font-medium mb-3">Create Bulk Plan</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Plan Name
                </label>
                <input
                  type="text"
                  value={newPlan.name}
                  onChange={(e) => setNewPlan({ ...newPlan, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
                  placeholder="e.g., Accept all low severity in dev"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Description
                </label>
                <textarea
                  value={newPlan.description}
                  onChange={(e) => setNewPlan({ ...newPlan, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
                  rows={2}
                  placeholder="Describe what this plan will do..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Action
                </label>
                <select
                  value={newPlan.action}
                  onChange={(e) => setNewPlan({ ...newPlan, action: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
                >
                  <option value="accept_risk">Accept Risk</option>
                  <option value="false_positive">Mark as False Positive</option>
                  <option value="needs_remediation">Needs Remediation</option>
                  <option value="escalate">Escalate</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Filter by Severity
                </label>
                <div className="flex gap-2">
                  {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map(severity => (
                    <label key={severity} className="flex items-center">
                      <input
                        type="checkbox"
                        checked={newPlan.filters.severity.includes(severity)}
                        onChange={(e) => {
                          const newSeverities = e.target.checked
                            ? [...newPlan.filters.severity, severity]
                            : newPlan.filters.severity.filter((s: string) => s !== severity);
                          setNewPlan({
                            ...newPlan,
                            filters: { ...newPlan.filters, severity: newSeverities }
                          });
                        }}
                        className="mr-1"
                      />
                      <span className="text-sm">{severity}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Filter by Namespace (optional)
                </label>
                <input
                  type="text"
                  value={newPlan.filters.namespace}
                  onChange={(e) => setNewPlan({
                    ...newPlan,
                    filters: { ...newPlan.filters, namespace: e.target.value }
                  })}
                  className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
                  placeholder="e.g., development"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Filter by Package Pattern (optional)
                </label>
                <input
                  type="text"
                  value={newPlan.filters.package_pattern}
                  onChange={(e) => setNewPlan({
                    ...newPlan,
                    filters: { ...newPlan.filters, package_pattern: e.target.value }
                  })}
                  className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
                  placeholder="e.g., ^test-*"
                />
              </div>

              <div className="flex gap-2">
                <Button onClick={handleCreatePlan} size="sm">
                  Create Plan
                </Button>
                <Button
                  onClick={() => setShowCreateForm(false)}
                  size="sm"
                  variant="outline"
                >
                  Cancel
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Plans List */}
        {!bulkPlans || bulkPlans.length === 0 ? (
          <div className="text-center py-8 text-text-tertiary">
            <p>No bulk plans created yet.</p>
            <p className="text-sm mt-2">Create a plan to apply actions to multiple vulnerabilities at once.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {bulkPlans.map((plan) => {
              const isExpanded = expandedPlans.has(plan.id!);

              return (
                <div
                  key={plan.id}
                  className="border rounded-lg p-4 hover:shadow-sm transition-shadow"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="font-medium text-text-primary">{plan.name}</h3>
                        <Badge
                          variant="info"
                          className={`text-xs ${getActionColor(plan.action)}`}
                        >
                          {getActionLabel(plan.action)}
                        </Badge>
                        {plan.status === 'executed' && (
                          <Badge variant="default" className="text-xs bg-success">
                            Executed
                          </Badge>
                        )}
                      </div>

                      {plan.description && (
                        <p className="text-sm text-text-secondary mb-2">{plan.description}</p>
                      )}

                      <div className="flex items-center gap-4 text-sm text-text-tertiary">
                        <span>Affects: {plan.actual_count || plan.estimated_count || 0} vulnerabilities</span>
                        {plan.executed_at && (
                          <span>Executed: {formatDate(plan.executed_at)}</span>
                        )}
                      </div>

                      {/* Expandable Filters */}
                      {plan.filters && (
                        <button
                          onClick={() => togglePlanExpanded(plan.id!)}
                          className="mt-2 text-sm text-text-secondary hover:text-text-primary flex items-center gap-1"
                        >
                          {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                          View Filters
                        </button>
                      )}

                      {isExpanded && plan.filters && (
                        <div className="mt-2 p-3 bg-bg-secondary rounded text-sm">
                          <pre className="text-xs text-text-secondary">
                            {JSON.stringify(plan.filters, null, 2)}
                          </pre>
                        </div>
                      )}
                    </div>

                    <div className="flex gap-2 ml-4">
                      {plan.status !== 'executed' && (
                        <Button
                          size="sm"
                          onClick={() => onExecutePlan(plan.id!)}
                          className="bg-success hover:bg-success/80"
                        >
                          <Play className="w-4 h-4 mr-1" />
                          Execute
                        </Button>
                      )}
                      {onDeletePlan && plan.status !== 'executed' && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => onDeletePlan(plan.id!)}
                          className="text-danger hover:bg-danger/10"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      )}
                    </div>
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