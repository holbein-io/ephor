import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { triageService } from '../services/api';
import { TriageSession } from '../types';

// Hook for managing triage sessions
export function useTriageSessions() {
  return useQuery({
    queryKey: ['enhanced-triage-sessions'],
    queryFn: () => triageService.getSessions()
  });
}

// Hook for fetching triage report data
export function useTriageReport(days: number, namespace?: string, enabled = true) {
  return useQuery({
    queryKey: ['triage-report', days, namespace],
    queryFn: () => triageService.getTriageReport(days, namespace),
    enabled
  });
}

// Hook for fetching preparations for a session
export function useTriagePreparations(sessionId: number | undefined, enabled = false) {
  return useQuery({
    queryKey: ['triage-preparations', sessionId],
    queryFn: () => triageService.getSessionPreparations(sessionId!),
    enabled: enabled && !!sessionId
  });
}

// Hook for fetching bulk plans for a session
export function useTriageBulkPlans(sessionId: number | undefined) {
  return useQuery({
    queryKey: ['triage-bulk-plans', sessionId],
    queryFn: () => triageService.getSessionBulkPlans(sessionId!),
    enabled: !!sessionId
  });
}

// Hook for fetching session metrics
export function useTriageSessionMetrics(sessionId: number | undefined, enabled = false) {
  return useQuery({
    queryKey: ['triage-session-metrics', sessionId],
    queryFn: () => triageService.getSessionMetrics(sessionId!),
    enabled: enabled && !!sessionId
  });
}

// Hook for creating a new triage session
export function useCreateTriageSession(onSessionCreated?: (session: TriageSession) => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionData: {
      session_date: string;
      status: string;
      prep_lead?: string;
      prep_notes?: string;
      attendees?: string[];
      notes?: string;
    }) => triageService.createSession(sessionData),
    onSuccess: async (data) => {
      queryClient.invalidateQueries({ queryKey: ['enhanced-triage-sessions'] });
      if (onSessionCreated && data?.id) {
        try {
          const session = await triageService.getSessionById(data.id);
          if (session && session.id) {
            onSessionCreated(session as TriageSession & { id: number });
          }
        } catch (error) {
          console.error('Failed to fetch created session:', error);
        }
      }
    },
    onError: (error: any) => {
      console.error('Failed to create triage session:', error);
    }
  });
}

// Hook for updating session status
export function useUpdateSessionStatus(currentSessionId?: number, onStatusUpdated?: (session: TriageSession) => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, status, user }: { sessionId: number; status: string; user?: string }) =>
      triageService.updateSessionStatus(sessionId, status, user || ''),
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: ['enhanced-triage-sessions'] });
      if (currentSessionId && onStatusUpdated) {
        const session = await triageService.getSessionById(currentSessionId);
        if (session.id !== undefined) {
          onStatusUpdated(session as TriageSession & { id: number });
        }
      }
    }
  });
}

// Hook for creating a preparation entry
export function useCreateTriagePreparation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (prepData: {
      session_id: number;
      vulnerability_id: number;
      prep_status?: string;
      prep_notes?: string;
      preliminary_decision?: string;
      priority_flag?: string;
      prep_by: string;
    }) => triageService.createPreparation(prepData as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['triage-preparations'] });
    }
  });
}

// Hook for creating a bulk plan
export function useCreateTriageBulkPlan() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (planData: {
      session_id: number;
      name: string;
      description?: string;
      filters: any;
      action: string;
      metadata?: any;
      created_by: string;
    }) => triageService.createBulkPlan(planData as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['triage-bulk-plans'] });
    }
  });
}

// Hook for executing a bulk plan
export function useExecuteBulkPlan() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ planId, executedBy = 'System' }: { sessionId?: number; planId: number; executedBy?: string }) =>
      triageService.executeBulkPlan(planId, executedBy),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['triage-bulk-plans'] });
      queryClient.invalidateQueries({ queryKey: ['triage-decisions'] });
    }
  });
}

// Hook for creating triage decisions
export function useCreateTriageDecision() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (decisionData: {
      session_id: number;
      vulnerability_id: number;
      status: string;
      notes?: string;
      decided_by: string;
      assigned_to?: string;
      target_date?: string;
      priority?: string;
    }) => triageService.createDecision(decisionData as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['triage-decisions'] });
      queryClient.invalidateQueries({ queryKey: ['triage-session-metrics'] });
    }
  });
}

// Hook for fetching triage decisions for a session
export function useTriageDecisions(sessionId: number | undefined) {
  return useQuery({
    queryKey: ['triage-decisions', sessionId],
    queryFn: () => triageService.getSessionDecisions(sessionId!),
    enabled: !!sessionId
  });
}