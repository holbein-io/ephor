import { useCallback, useState } from 'react';
import { TriageSessionManager } from '../components/triage/TriageSessionManager';
import { TriagePreparation } from '../components/triage/TriagePreparation';
import { TriageDecisionMaker } from '../components/triage/TriageDecisionMaker';
import { TriageBulkOperations } from '../components/triage/TriageBulkOperations';
import { TriageSessionReview } from '../components/triage/TriageSessionReview';
import { TriageWorkflowStepper } from '../components/triage/TriageWorkflowStepper';
import {
  useTriageSessions,
  useTriagePreparations,
  useTriageBulkPlans,
  useTriageDecisions,
  useCreateTriageSession,
  useUpdateSessionStatus,
  useCreateTriagePreparation,
  useCreateTriageDecision,
  useCreateTriageBulkPlan,
  useExecuteBulkPlan
} from '../hooks/useTriageData';
import { useTriageState } from '../hooks/useTriageState';
import { useUser } from '../contexts/UserContext';
import { TriageSession, Vulnerability } from '../types';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { AlertCircle, Clock, CheckCircle2 } from 'lucide-react';

/**
 * Triage component - refactored with composition pattern
 * Splits functionality into smaller, focused components
 */
export function Triage() {
  // Get user context for pre-filling prep lead
  const { displayName } = useUser();

  // Use the custom state management hook with user's display name as initial prepLead
  const { state, dispatch } = useTriageState({ prepLead: displayName });
  const [prepMaximized, setPrepMaximized] = useState(false);

  // Data fetching hooks
  const { data: sessions, isLoading: sessionsLoading } = useTriageSessions();

  const { data: preparations, isLoading: preparationsLoading } = useTriagePreparations(
    state.currentSession?.id,
    !!state.currentSession && (state.currentSession.status === 'PREPARING' || state.currentSession.status === 'ACTIVE' || state.currentSession.status === 'COMPLETED')
  );

  const { data: bulkPlans, isLoading: bulkPlansLoading } = useTriageBulkPlans(
    state.currentSession?.id
  );

  const { data: decisions, isLoading: decisionsLoading } = useTriageDecisions(
    state.currentSession?.id
  );

  // Mutation hooks
  const createSessionMutation = useCreateTriageSession((session) => {
    dispatch({ type: 'SET_CURRENT_SESSION', payload: session });
  });

  const updateSessionStatusMutation = useUpdateSessionStatus(
    state.currentSession?.id,
    (session) => {
      dispatch({ type: 'SET_CURRENT_SESSION', payload: session });
    }
  );

  const createPreparationMutation = useCreateTriagePreparation();
  const createDecisionMutation = useCreateTriageDecision();
  const createBulkPlanMutation = useCreateTriageBulkPlan();
  const executeBulkPlanMutation = useExecuteBulkPlan();

  // Callbacks
  const handleSelectSession = useCallback((session: TriageSession) => {
    dispatch({ type: 'SET_CURRENT_SESSION', payload: session });
  }, [dispatch]);

  const handleCreateSession = useCallback(() => {
    const sessionData = {
      session_date: new Date().toISOString(),
      status: 'PREPARING',
      prep_lead: state.prepLead || 'System',
      prep_notes: state.prepNotes || '',
      attendees: state.attendees.filter(a => a).length > 0 ? state.attendees.filter(a => a) : [],
      notes: state.sessionNotes || ''
    };
    createSessionMutation.mutate(sessionData);
  }, [state, createSessionMutation]);

  const handleAddToSession = useCallback((vulnerability: Vulnerability, priority?: string, notes?: string) => {
    if (!state.currentSession) return;

    const prepData = {
      session_id: state.currentSession.id,
      vulnerability_id: vulnerability.id!,
      prep_status: 'pending',
      prep_notes: notes,
      preliminary_decision: undefined,
      priority_flag: priority,
      prep_by: state.prepLead || 'System'
    };

    createPreparationMutation.mutate(prepData);
  }, [state.currentSession, state.prepLead, createPreparationMutation]);

  const handleCreateBulkPlan = useCallback((plan: any) => {
    if (!state.currentSession) return;

    createBulkPlanMutation.mutate({
      session_id: state.currentSession.id,
      created_by: state.prepLead || 'System',
      ...plan
    });
  }, [state.currentSession, state.prepLead, createBulkPlanMutation]);

  const handleExecuteBulkPlan = useCallback((planId: number) => {
    if (!state.currentSession) return;

    executeBulkPlanMutation.mutate({
      sessionId: state.currentSession.id,
      planId
    });
  }, [state.currentSession, executeBulkPlanMutation]);

  const handleStartSession = useCallback(() => {
    if (!state.currentSession) return;

    updateSessionStatusMutation.mutate({
      sessionId: state.currentSession.id,
      status: 'ACTIVE',
      user: state.prepLead || 'System'
    });
  }, [state.currentSession, state.prepLead, updateSessionStatusMutation]);

  const handleCompleteSession = useCallback(() => {
    if (!state.currentSession) return;

    updateSessionStatusMutation.mutate({
      sessionId: state.currentSession.id,
      status: 'COMPLETED',
      user: state.prepLead || 'System'
    });
  }, [state.currentSession, state.prepLead, updateSessionStatusMutation]);

  const handleMakeDecision = useCallback((vulnerabilityId: number, decision: any) => {
    if (!state.currentSession) return;

    createDecisionMutation.mutate({
      session_id: state.currentSession.id,
      vulnerability_id: vulnerabilityId,
      status: decision.status,
      notes: decision.notes,
      decided_by: state.prepLead || 'System',
      assigned_to: decision.assigned_to,
      target_date: decision.target_date,
      priority: decision.priority
    });
  }, [state.currentSession, state.prepLead, createDecisionMutation]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-display font-bold text-text-primary">Triage</h1>
        <div className="flex gap-2">
          {state.currentSession && state.currentSession.status === 'PREPARING' && (
            <Button onClick={handleStartSession} className="bg-success hover:bg-success/80">
              <CheckCircle2 className="w-4 h-4 mr-2" />
              Start Session
            </Button>
          )}
          {state.currentSession && state.currentSession.status === 'ACTIVE' && (
            <Button onClick={handleCompleteSession}>
              <CheckCircle2 className="w-4 h-4 mr-2" />
              Complete Session
            </Button>
          )}
        </div>
      </div>

      {/* Workflow Stepper */}
      {state.currentSession && (
        <Card>
          <CardContent className="pt-6">
            <TriageWorkflowStepper
              currentStatus={state.currentSession.status}
              preparationsCount={preparations?.length || 0}
              decisionsCount={state.currentSession.decisions_count || 0}
            />

            {/* Quick Stats Row */}
            <div className="grid grid-cols-3 gap-4 pt-4 border-t mt-4">
              <div className="text-center">
                <div className="text-xl font-bold text-text-primary">
                  {preparations?.length || 0}
                </div>
                <div className="text-xs text-text-tertiary">Prepared</div>
              </div>
              <div className="text-center">
                <div className="text-xl font-bold text-text-primary">
                  {decisions?.length || 0}
                </div>
                <div className="text-xs text-text-tertiary">Decisions</div>
              </div>
              <div className="text-center">
                <div className="text-xl font-bold text-text-primary">
                  {bulkPlans?.filter(p => p.status === 'executed').length || 0}/{bulkPlans?.length || 0}
                </div>
                <div className="text-xs text-text-tertiary">Bulk Plans</div>
              </div>
            </div>

            {state.currentSession.session_duration_minutes && (
              <div className="mt-3 pt-3 border-t">
                <div className="flex items-center justify-center gap-2 text-sm text-text-secondary">
                  <Clock className="w-4 h-4" />
                  <span>Duration: {state.currentSession.session_duration_minutes} minutes</span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <div className={prepMaximized ? '' : 'grid grid-cols-1 lg:grid-cols-3 gap-6'}>
        {/* Sessions List */}
        {!prepMaximized && (
          <div className="lg:col-span-1">
            <TriageSessionManager
              sessions={sessions as any}
              currentSession={state.currentSession}
              isLoading={sessionsLoading}
              onSelectSession={handleSelectSession}
              onCreateSession={handleCreateSession}
            />
          </div>
        )}

        {/* Main Content Area */}
        <div className={prepMaximized ? '' : 'lg:col-span-2 space-y-6'}>
          {state.currentSession ? (
            <>
              {/* Preparation Phase */}
              {state.currentSession.status === 'PREPARING' && (
                <TriagePreparation
                  preparations={preparations as any}
                  sessionId={state.currentSession.id}
                  onAddToSession={handleAddToSession}
                  maximized={prepMaximized}
                  onToggleMaximize={() => setPrepMaximized(prev => !prev)}
                />
              )}

              {/* Bulk Operations - Only show for active sessions */}
              {state.currentSession.status !== 'COMPLETED' && (
                <TriageBulkOperations
                  bulkPlans={bulkPlans}
                  isLoading={bulkPlansLoading}
                  sessionId={state.currentSession.id}
                  onCreatePlan={handleCreateBulkPlan}
                  onExecutePlan={handleExecuteBulkPlan}
                />
              )}

              {/* Active Session - Decision Making */}
              {state.currentSession.status === 'ACTIVE' && (
                <TriageDecisionMaker
                  preparations={preparations as any}
                  isLoading={preparationsLoading}
                  sessionId={state.currentSession.id}
                  onMakeDecision={handleMakeDecision}
                />
              )}

              {/* Completed Session - Review */}
              {state.currentSession.status === 'COMPLETED' && (
                <TriageSessionReview
                  decisions={decisions as any}
                  preparations={preparations as any}
                  isLoadingDecisions={decisionsLoading}
                  isLoadingPreparations={preparationsLoading}
                />
              )}
            </>
          ) : (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-16">
                <div className="bg-accent/10 rounded-full p-4 mb-4">
                  <AlertCircle className="w-10 h-10 text-accent" />
                </div>
                <h3 className="text-lg font-semibold text-text-primary mb-2">No session selected</h3>
                <p className="text-sm text-text-tertiary text-center max-w-md mb-6">
                  Select an existing triage session from the list on the left, or create a new session to start reviewing and triaging vulnerabilities.
                </p>
                <div className="flex items-center gap-4 text-sm text-text-tertiary">
                  <span className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-warning" />
                    Preparing
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-accent" />
                    Active
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-success" />
                    Completed
                  </span>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
