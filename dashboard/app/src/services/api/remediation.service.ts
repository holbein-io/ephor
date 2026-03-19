import { apiClient } from './client';
import {
  Remediation,
  RemediationComment,
  RemediationWithDetails,
  RemediationStatus,
  RemediationPriority,
  CompletionMethod
} from '../../types';

export const remediationService = {
  async getByVulnerability(vulnerabilityId: number): Promise<RemediationWithDetails[]> {
    return apiClient.get(`/remediations/vulnerability/${vulnerabilityId}`);
  },

  async getById(id: number): Promise<Remediation & { comments: RemediationComment[] }> {
    return apiClient.get(`/remediations/${id}`);
  },

  async update(
    id: number,
    updates: {
      assigned_to?: string | null;
      target_date?: string | null;
      priority?: RemediationPriority;
      notes?: string | null;
    }
  ): Promise<Remediation> {
    return apiClient.patch(`/remediations/${id}`, updates);
  },

  async changeStatus(
    id: number,
    request: {
      status: RemediationStatus;
      completionMethod?: CompletionMethod;
      completedBy?: string;
      notes?: string;
    }
  ): Promise<Remediation> {
    return apiClient.patch(`/remediations/${id}/status`, {
      status: request.status,
      completion_method: request.completionMethod,
      completed_by: request.completedBy,
      notes: request.notes
    });
  },

  async addComment(
    remediationId: number,
    author: string,
    comment: string
  ): Promise<RemediationComment> {
    return apiClient.post(`/remediations/${remediationId}/comments`, { author, comment });
  },

  async getComments(remediationId: number): Promise<RemediationComment[]> {
    return apiClient.get(`/remediations/${remediationId}/comments`);
  },

  async getStatistics(): Promise<{
    total: number;
    planned: number;
    in_progress: number;
    completed: number;
    abandoned: number;
    overdue: number;
    completion_rate: number;
    avg_completion_days: number;
  }> {
    return apiClient.get('/remediations/statistics');
  },

  async getOverdue(): Promise<Remediation[]> {
    return apiClient.get('/remediations/overdue');
  }
};
