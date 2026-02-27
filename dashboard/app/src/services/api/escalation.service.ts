import { apiClient } from './client';
import { Escalation } from '../../types';

/**
 * Service for escalation-related API operations
 */
export const escalationService = {
  /**
   * Get all escalations
   */
  async getEscalations(): Promise<Escalation[]> {
    return apiClient.get('/escalations');
  },

  /**
   * Create new escalation
   */
  async createEscalation(escalation: {
    vulnerability_id: number;
    escalation_level: string;
    escalated_by: string;
    reason: string;
  }): Promise<{ id: number; message: string }> {
    return apiClient.post('/escalations', escalation);
  },

  /**
   * Update escalation
   */
  async updateEscalation(
    id: number,
    updates: {
      status?: 'pending' | 'acknowledged' | 'resolved';
      ms_teams_message_id?: string;
    }
  ): Promise<{ message: string }> {
    return apiClient.patch(`/escalations/${id}`, updates);
  }
};