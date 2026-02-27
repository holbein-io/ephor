import { apiClient } from './client';
import {
  DashboardMetrics,
  VulnerabilityTrend,
  NamespaceComparison
} from '../../types';

export const dashboardService = {
  async getMetrics(): Promise<DashboardMetrics> {
    return apiClient.get('/dashboard/metrics');
  },

  async getTrends(days: number = 30): Promise<VulnerabilityTrend[]> {
    return apiClient.get('/dashboard/trends', { days });
  },

  async getNamespaces(): Promise<string[]> {
    return apiClient.get('/dashboard/namespaces');
  },

  async getNamespaceComparison(): Promise<NamespaceComparison[]> {
    return apiClient.get('/dashboard/namespace-comparison');
  }
};
