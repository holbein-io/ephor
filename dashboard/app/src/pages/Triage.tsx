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
import { AlertCircle, CheckCircle2 } from 'lucide-react';

export function Triage() {
  const { displayName } = useUser();
  const { state, dispatch } = useTriageState({ prepLead: displayName });
  const [prepMaximized, setPrepMaximized] = useState(false);

  const { data: sessions, isLoading: sessionsLoading } = useTriageSessions();
  const { data: preparations, isLoading: preparationsLoading } = useTriagePreparations(
    state.currentSession?.id,
    !!state.currentSession && ['PREPARING', 'ACTIVE', 'COMPLETED'].includes(state.currentSession.status)
  );
  const { data: bulkPlans, isLoading: bulkPlansLoading } = useTriageBulkPlans(state.currentSession?.id);
  const { data: decisions, isLoading: decisionsLoading } = useTriageDecisions(state.currentSession?.id);

  const createSessionMutation = useCreateTriageSession((session) => {
    dispatch({ type: 'SET_CURRENT_SESSION', payload: session });
  });
  const updateSessionStatusMutation = useUpdateSessionStatus(state.currentSession?.id, (session) => {
    dispatch({ type: 'SET_CURRENT_SESSION', payload: session });
  });
  const createPreparationMutation = useCreateTriagePreparation();
  const createDecisionMutation = useCreateTriageDecision();
  const createBulkPlanMutation = useCreateTriageBulkPlan();
  const executeBulkPlanMutation = useExecuteBulkPlan();

  const handleSelectSession = useCallback((session: TriageSession) => {
    dispatch({ type: 'SET_CURRENT_SESSION', payload: session });
  }, [dispatch]);

  const handleCreateSession = useCallback(() => {
    createSessionMutation.mutate({
      session_date: new Date().toISOString(),
      status: 'PREPARING',
      prep_lead: state.prepLead || 'System',
      prep_notes: state.prepNotes || '',
      attendees: state.attendees.filter(a => a).length > 0 ? state.attendees.filter(a => a) : [],
      notes: state.sessionNotes || ''
    });
  }, [state, createSessionMutation]);

  const handleAddToSession = useCallback((vulnerability: Vulnerability, priority?: string, notes?: string) => {
    if (!state.currentSession) return;
    createPreparationMutation.mutate({
      session_id: state.currentSession.id,
      vulnerability_id: vulnerability.id!,
      prep_status: 'pending',
      prep_notes: notes,
      preliminary_decision: undefined,
      priority_flag: priority,
      prep_by: state.prepLead || 'System'
    });
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
    executeBulkPlanMutation.mutate({ sessionId: state.currentSession.id, planId });
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

  const prepCount = preparations?.length || 0;
  const decCount = decisions?.length || 0;
  const remaining = Math.max(0, prepCount - decCount);

  return (
    <div className="-mx-6 -mt-7 flex" style={{ height: 'calc(100vh - 56px)' }}>
      {/* Session Panel (sidebar) */}
      {!prepMaximized && (
        <TriageSessionManager
          sessions={sessions as any}
          currentSession={state.currentSession}
          isLoading={sessionsLoading}
          onSelectSession={handleSelectSession}
          onCreateSession={handleCreateSession}
        />
      )}

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto px-7 py-6 flex flex-col gap-4">
        {state.currentSession ? (
          <>
            {/* Action buttons */}
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl italic text-text-primary">
                Triage Session
              </h2>
              <div className="flex gap-2">
                {state.currentSession.status === 'PREPARING' && (
                  <button
                    onClick={handleStartSession}
                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-accent-mint text-bg-primary text-sm font-semibold hover:bg-accent-mint/80 transition-colors"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    Start Session
                  </button>
                )}
                {state.currentSession.status === 'ACTIVE' && (
                  <button
                    onClick={handleCompleteSession}
                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-accent text-white text-sm font-semibold hover:bg-accent-hover transition-colors"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    Complete Session
                  </button>
                )}
              </div>
            </div>

            {/* Stepper */}
            <TriageWorkflowStepper
              currentStatus={state.currentSession.status}
              preparationsCount={prepCount}
              decisionsCount={state.currentSession.decisions_count || 0}
            />

            {/* Stats Row */}
            <div className="grid grid-cols-4 gap-2.5 animate-fade-up delay-2">
              {[
                { label: 'Prepared', value: prepCount, color: 'text-text-primary' },
                { label: 'Decided', value: decCount, color: 'text-accent-mint' },
                { label: 'Remaining', value: remaining, color: 'text-accent' },
                { label: 'Duration', value: state.currentSession.session_duration_minutes ? `${state.currentSession.session_duration_minutes}m` : '-', color: 'text-accent-cool' },
              ].map(stat => (
                <div key={stat.label} className="bg-bg-secondary border border-border rounded-2xl px-[18px] py-3.5">
                  <div className="font-mono text-[11px] tracking-[0.06em] uppercase text-text-tertiary mb-1">{stat.label}</div>
                  <div className={`text-[22px] font-semibold leading-none ${stat.color}`}>{stat.value}</div>
                </div>
              ))}
            </div>

            {/* Phase Content */}
            {state.currentSession.status === 'PREPARING' && (
              <TriagePreparation
                preparations={preparations as any}
                sessionId={state.currentSession.id}
                onAddToSession={handleAddToSession}
                maximized={prepMaximized}
                onToggleMaximize={() => setPrepMaximized(prev => !prev)}
              />
            )}

            {state.currentSession.status !== 'COMPLETED' && (
              <TriageBulkOperations
                bulkPlans={bulkPlans}
                isLoading={bulkPlansLoading}
                sessionId={state.currentSession.id}
                onCreatePlan={handleCreateBulkPlan}
                onExecutePlan={handleExecuteBulkPlan}
              />
            )}

            {state.currentSession.status === 'ACTIVE' && (
              <TriageDecisionMaker
                preparations={preparations as any}
                isLoading={preparationsLoading}
                sessionId={state.currentSession.id}
                onMakeDecision={handleMakeDecision}
              />
            )}

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
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="bg-accent/10 rounded-full p-4 mb-4">
              <AlertCircle className="w-10 h-10 text-accent" />
            </div>
            <h3 className="text-lg font-semibold text-text-primary mb-2">No session selected</h3>
            <p className="text-sm text-text-tertiary text-center max-w-md mb-6">
              Select an existing triage session from the panel on the left, or create a new session to start reviewing vulnerabilities.
            </p>
            <div className="flex items-center gap-4 text-sm text-text-tertiary">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-warning" /> Preparing
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-accent" /> Active
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-success" /> Completed
              </span>
            </div>
          </div>
        )}

      </main>
    </div>
  );
}
