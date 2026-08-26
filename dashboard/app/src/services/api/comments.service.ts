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

export const commentsService = {
  async getVulnerabilityComments(vulnerabilityId: number): Promise<CommentDto[]> {
    return apiClient.get(`/comments/vulnerabilities/${vulnerabilityId}`);
  },

  async addVulnerabilityComment(vulnerabilityId: number, body: string): Promise<CommentDto> {
    return apiClient.post(`/comments/vulnerabilities/${vulnerabilityId}`, { body });
  },

  async getEscalationComments(escalationId: number): Promise<CommentDto[]> {
    return apiClient.get(`/comments/escalations/${escalationId}`);
  },

  async addEscalationComment(escalationId: number, body: string): Promise<CommentDto> {
    return apiClient.post(`/comments/escalations/${escalationId}`, { body });
  },

  async updateComment(commentId: number, body: string): Promise<CommentDto> {
    return apiClient.put(`/comments/${commentId}`, { body });
  },

  async deleteComment(commentId: number): Promise<void> {
    return apiClient.delete(`/comments/${commentId}`);
  },
};
