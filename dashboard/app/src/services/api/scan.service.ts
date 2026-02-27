import { apiClient } from './client';
import { Scan } from '../../types';

/**
 * Service for scan-related API operations
 */
export const scanService = {
  /**
   * Get list of scans
   */
  async getScans(limit: number = 20): Promise<Scan[]> {
    return apiClient.get('/scans', { limit });
  },

  /**
   * Get scan by ID
   */
  async getScanById(id: number): Promise<Scan> {
    return apiClient.get(`/scans/${id}`);
  },

};