import { apiClient } from './client';

export interface AuditLogEntry {
  id: number;
  action: string;
  entity_type: string;
  entity_id: number;
  performed_by: string;
  details: string | null;
  created_at: string;
}

export interface AuditLogParams {
  entity_type?: string;
  entity_id?: number;
  actor?: string;
  action?: string;
  from?: string;
  to?: string;
}

export const auditService = {
  async getAuditLog(params?: AuditLogParams): Promise<AuditLogEntry[]> {
    return apiClient.get('/audit', { params });
  },

  async getEntityActivity(entityType: string, entityId: number): Promise<AuditLogEntry[]> {
    return apiClient.get('/audit', {
      params: { entity_type: entityType, entity_id: entityId },
    });
  },
};
