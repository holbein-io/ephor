import { apiClient } from './client';

export interface DirectoryCapabilities {
  provider: string;
  user_search_enabled: boolean;
  strict_assignment: boolean;
  my_items_enabled: boolean;
  user_sync_enabled: boolean;
}

export interface KnownUserDto {
  username: string;
  email: string;
  display_name: string;
  groups_csv: string;
  first_seen_at: string;
  last_seen_at: string;
}

export interface MyItemsResponse {
  remediations: Array<{ id: number; vulnerability_id: number; cve_id: string; status: string; target_date: string }>;
  escalations: Array<{ id: number; vulnerability_id: number; cve_id: string; status: string; escalated_at: string }>;
  recent_comments: Array<{ id: number; entity_type: string | null; entity_id: number | null; vulnerability_id: number | null; cve_id: string | null; body: string; created_at: string }>;
}

/**
 * Service for user directory operations
 */
export const usersService = {
  /**
   * Get directory provider capabilities
   */
  async getCapabilities(): Promise<DirectoryCapabilities> {
    return apiClient.get('/users/capabilities');
  },

  /**
   * Search known users by query
   */
  async searchUsers(query: string, limit = 10): Promise<KnownUserDto[]> {
    return apiClient.get(`/users/search?q=${encodeURIComponent(query)}&limit=${limit}`);
  },

  /**
   * Get items assigned to the current user
   */
  async getMyItems(): Promise<MyItemsResponse> {
    return apiClient.get('/users/me/items');
  },
};
