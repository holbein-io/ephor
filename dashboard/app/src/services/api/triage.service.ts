import { apiClient } from './client';
import {
  TriageSession,
  TriageDecision,
  TriagePreparation,
  TriageBulkPlan,
  Vulnerability
} from '../../types';

export interface TriageReport {
  days: number;
  namespace?: string;
  vulnerabilities: Vulnerability[];
  total: number;
}

export interface TriageSessionMetrics {
  total_vulnerabilities: number;
  decisions_made: number;
  accepted_risk: number;
  false_positive: number;
  needs_remediation: number;
  duplicate: number;
  average_decision_time?: number;
}

/**
 * Service for triage-related API operations
 */
export const triageService = {
  /**
   * Get triage report for preparation
   */
  async getTriageReport(days: number = 7, namespace?: string): Promise<TriageReport> {
    return apiClient.get('/triage/report', { days, namespace });
  },

  /**
   * Get all triage sessions
   */
  async getSessions(): Promise<TriageSession[]> {
    return apiClient.get('/triage/sessions');
  },

  /**
   * Get specific triage session with decisions
   */
  async getSessionById(id: number): Promise<TriageSession> {
    return apiClient.get(`/triage/sessions/${id}`);
  },

  /**
   * Create new triage session
   */
  async createSession(session: {
    session_date: string;
    status: string;
    prep_lead?: string;
    prep_notes?: string;
    attendees?: string[];
    notes?: string;
  }): Promise<{
    id: number;
    message: string;
  }> {
    return apiClient.post('/triage/sessions', session);
  },

  /**
   * Update triage session
   */
  async updateSession(
    id: number,
    updates: Partial<TriageSession>
  ): Promise<{ message: string }> {
    return apiClient.patch(`/triage/sessions/${id}`, updates);
  },

  /**
   * Update triage session status
   */
  async updateSessionStatus(
    id: number,
    status: string,
    user: string
  ): Promise<TriageSession> {
    return apiClient.patch(`/triage/sessions/${id}/status`, { status, user });
  },

  /**
   * Delete triage session
   */
  async deleteSession(id: number): Promise<{ message: string }> {
    return apiClient.delete(`/triage/sessions/${id}`);
  },

  /**
   * Create triage decision
   */
  async createDecision(decision: Omit<TriageDecision, 'id' | 'created_at'>): Promise<{
    id: number;
    message: string;
  }> {
    return apiClient.post('/triage/decisions', decision);
  },

  /**
   * Get triage decisions for a session
   */
  async getSessionDecisions(sessionId: number): Promise<TriageDecision[]> {
    return apiClient.get(`/triage/sessions/${sessionId}/decisions`);
  },

  /**
   * Create triage preparation
   */
  async createPreparation(
    preparation: Omit<TriagePreparation, 'id' | 'created_at'>
  ): Promise<{ id: number; message: string }> {
    return apiClient.post('/triage/preparations', preparation);
  },

  /**
   * Get preparations for a session
   */
  async getSessionPreparations(sessionId: number): Promise<TriagePreparation[]> {
    return apiClient.get(`/triage/sessions/${sessionId}/preparations`);
  },

  /**
   * Delete preparation
   */
  async deletePreparation(id: number): Promise<{ message: string }> {
    return apiClient.delete(`/triage/preparations/${id}`);
  },

  /**
   * Create bulk plan
   */
  async createBulkPlan(plan: Omit<TriageBulkPlan, 'id' | 'created_at'>): Promise<{
    id: number;
    message: string;
  }> {
    return apiClient.post('/triage/bulk-plans', plan);
  },

  /**
   * Get bulk plans for a session
   */
  async getSessionBulkPlans(sessionId: number): Promise<TriageBulkPlan[]> {
    return apiClient.get(`/triage/sessions/${sessionId}/bulk-plans`);
  },

  /**
   * Execute bulk plan
   */
  async executeBulkPlan(
    planId: number,
    executedBy: string
  ): Promise<{ affected: number; message: string }> {
    return apiClient.post(`/triage/bulk-plans/${planId}/execute`, { executedBy });
  },

  /**
   * Get session metrics
   */
  async getSessionMetrics(sessionId: number): Promise<TriageSessionMetrics> {
    return apiClient.get(`/triage/sessions/${sessionId}/metrics`);
  },

  /**
   * Complete triage session
   */
  async completeSession(sessionId: number): Promise<{
    message: string;
    metrics: TriageSessionMetrics;
  }> {
    return apiClient.post(`/triage/sessions/${sessionId}/complete`);
  }
};