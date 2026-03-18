import { apiClient } from './client';

export interface CommentDto {
  id: number;
  entityType: string;
  entityId: number;
  body: string;
  commentType: string | null;
  createdBy: string;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string | null;
}

/**
 * Service for polymorphic comment operations
 */
export const commentsService = {
  /**
   * Get comments for a vulnerability
   */
  async getVulnerabilityComments(vulnerabilityId: number): Promise<CommentDto[]> {
    return apiClient.get(`/comments/vulnerabilities/${vulnerabilityId}`);
  },

  /**
   * Add comment to a vulnerability
   */
  async addVulnerabilityComment(vulnerabilityId: number, body: string): Promise<CommentDto> {
    return apiClient.post(`/comments/vulnerabilities/${vulnerabilityId}`, { body });
  },

  /**
   * Get comments for an escalation
   */
  async getEscalationComments(escalationId: number): Promise<CommentDto[]> {
    return apiClient.get(`/comments/escalations/${escalationId}`);
  },

  /**
   * Add comment to an escalation
   */
  async addEscalationComment(escalationId: number, body: string): Promise<CommentDto> {
    return apiClient.post(`/comments/escalations/${escalationId}`, { body });
  },

  /**
   * Update a comment
   */
  async updateComment(commentId: number, body: string): Promise<CommentDto> {
    return apiClient.put(`/comments/${commentId}`, { body });
  },

  /**
   * Delete a comment
   */
  async deleteComment(commentId: number): Promise<void> {
    return apiClient.delete(`/comments/${commentId}`);
  },
};
