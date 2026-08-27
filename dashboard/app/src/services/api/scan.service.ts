import { apiClient } from './client';
import { Scan } from '../../types';

export const scanService = {
  async getScans(limit: number = 20): Promise<Scan[]> {
    return apiClient.get('/scans', { limit });
  },

  async getScanById(id: number): Promise<Scan> {
    return apiClient.get(`/scans/${id}`);
  },

};