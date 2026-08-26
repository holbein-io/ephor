import { apiClient } from './client';
import { Escalation } from '../../types';

export const escalationService = {
  async getEscalations(): Promise<Escalation[]> {
    return apiClient.get('/escalations');
  },

  async createEscalation(escalation: {
    vulnerability_id: number;
    escalation_level: string;
    escalated_by: string;
    reason: string;
  }): Promise<{ id: number; message: string }> {
    return apiClient.post('/escalations', escalation);
  },

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