import { apiClient } from './client';
import {
  Remediation,
  RemediationComment,
  RemediationWithDetails,
  RemediationStatus,
  RemediationPriority
} from '../../types';

/**
 * Service for remediation-related API operations
 */
export const remediationService = {
  /**
   * Get all remediations for a vulnerability (history view)
   */
  async getByVulnerability(vulnerabilityId: number): Promise<RemediationWithDetails[]> {
    return apiClient.get(`/remediations/vulnerability/${vulnerabilityId}`);
  },

  /**
   * Get a single remediation by ID
   */
  async getById(id: number): Promise<Remediation & { comments: RemediationComment[] }> {
    return apiClient.get(`/remediations/${id}`);
  },

  /**
   * Update a remediation
   */
  async update(
    id: number,
    updates: {
      status?: RemediationStatus;
      assigned_to?: string | null;
      target_date?: string | null;
      priority?: RemediationPriority;
      notes?: string | null;
      completion_method?: 'auto_resolved' | 'manual' | 'version_upgrade';
      completed_by?: string | null;
    }
  ): Promise<Remediation> {
    return apiClient.patch(`/remediations/${id}`, updates);
  },

  /**
   * Add a comment to a remediation
   */
  async addComment(
    remediationId: number,
    author: string,
    comment: string
  ): Promise<RemediationComment> {
    return apiClient.post(`/remediations/${remediationId}/comments`, { author, comment });
  },

  /**
   * Get comments for a remediation
   */
  async getComments(remediationId: number): Promise<RemediationComment[]> {
    return apiClient.get(`/remediations/${remediationId}/comments`);
  },

  /**
   * Get remediation statistics
   */
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

  /**
   * Get overdue remediations
   */
  async getOverdue(): Promise<Remediation[]> {
    return apiClient.get('/remediations/overdue');
  }
};
