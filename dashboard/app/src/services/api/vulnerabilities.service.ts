import { apiClient } from './client';
import {
  Vulnerability,
  VulnerabilityFilters,
  PaginatedResponse,
  Comment,
  Workload
} from '../../types';

/**
 * Service for vulnerability-related API operations
 */
export const vulnerabilityService = {
  /**
   * Get paginated list of vulnerabilities with filters
   */
  async getVulnerabilities(
    filters?: VulnerabilityFilters
  ): Promise<PaginatedResponse<Vulnerability & { affected_workloads: number }>> {
    return apiClient.get('/vulnerabilities', filters);
  },

  /**
   * Get vulnerability by ID with associated workloads and computed status
   */
  async getById(id: number): Promise<Vulnerability & { workloads: Workload[]; status: string }> {
    return apiClient.get(`/vulnerabilities/${id}`);
  },

  /**
   * Update vulnerability status
   */
  async updateStatus(id: number, status: string, applyToAll = false): Promise<{ message: string }> {
    return apiClient.patch(`/vulnerabilities/${id}/status`, { status, apply_to_all: applyToAll });
  },

  /**
   * Get triage information for a vulnerability
   */
  async getTriageInfo(id: number): Promise<any> {
    return apiClient.get(`/vulnerabilities/${id}/triage-info`);
  },

  /**
   * Get comments for a vulnerability
   */
  async getComments(vulnerabilityId: number): Promise<Comment[]> {
    return apiClient.get(`/vulnerabilities/${vulnerabilityId}/comments`);
  },

  /**
   * Add comment to vulnerability
   */
  async addComment(
    vulnerabilityId: number,
    comment: { author: string; comment: string; comment_type: string }
  ): Promise<{ id: number; message: string }> {
    return apiClient.post(`/vulnerabilities/${vulnerabilityId}/comments`, comment);
  },

  /**
   * Delete comment
   */
  async deleteComment(
    vulnerabilityId: number,
    commentId: number
  ): Promise<{ message: string }> {
    return apiClient.delete(`/vulnerabilities/${vulnerabilityId}/comments/${commentId}`);
  },

  /**
   * Auto-resolve vulnerabilities
   */
  async autoResolve(
    gracePeriodDays: number = 30,
    dryRun: boolean = false
  ): Promise<{
    resolved: number;
    candidates: Array<{ id: number; cve_id: string; package_name: string; last_seen: string }>;
  }> {
    return apiClient.post('/vulnerabilities/auto-resolve', { gracePeriodDays, dryRun });
  },

  /**
   * Reopen auto-resolved vulnerability
   */
  async reopen(id: number): Promise<{ message: string }> {
    return apiClient.post(`/vulnerabilities/${id}/reopen`);
  },

  /**
   * Search vulnerabilities by CVE or package name
   */
  async search(query: string): Promise<PaginatedResponse<Vulnerability>> {
    return apiClient.get('/vulnerabilities', { search: query, limit: 20 });
  },

  /**
   * Get vulnerabilities by severity
   */
  async getBySeverity(
    severity: string[]
  ): Promise<PaginatedResponse<Vulnerability & { affected_workloads: number }>> {
    return apiClient.get('/vulnerabilities', { severity });
  },

  /**
   * Get vulnerabilities by namespace
   */
  async getByNamespace(
    namespace: string
  ): Promise<PaginatedResponse<Vulnerability & { affected_workloads: number }>> {
    return apiClient.get('/vulnerabilities', { namespace });
  }
};