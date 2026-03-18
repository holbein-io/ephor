import { apiClient } from './client';

export interface User {
  username: string;
  email: string;
  displayName?: string;
  groups: string[];
  permissions: string[];
}

export interface AuthResponse {
  authenticated: boolean;
  user?: User;
  error?: string;
}

export interface AuthConfig {
  authEnabled: boolean;
  loginUrl: string;
  logoutUrl: string;
  provider: string;
  idpLogoutUrl?: string;
}

export type Permission =
  | 'VIEW_VULNERABILITIES'
  | 'MANAGE_VULNERABILITIES'
  | 'VIEW_ESCALATIONS'
  | 'MANAGE_ESCALATIONS'
  | 'VIEW_TRIAGE'
  | 'MANAGE_TRIAGE'
  | 'VIEW_REMEDIATIONS'
  | 'MANAGE_REMEDIATIONS'
  | 'VIEW_ADMIN'
  | 'MANAGE_ADMIN';

export const authService = {
  /**
   * Get current user info from OAuth2 proxy headers
   */
  async getCurrentUser(): Promise<AuthResponse> {
    try {
      return await apiClient.get<AuthResponse>('/auth/me');
    } catch {
      return { authenticated: false };
    }
  },

  /**
   * Get auth status (lighter endpoint, doesn't require auth)
   */
  async getAuthStatus(): Promise<AuthResponse> {
    try {
      return await apiClient.get<AuthResponse>('/auth/status');
    } catch {
      return { authenticated: false };
    }
  },

  /**
   * Get auth configuration
   */
  async getAuthConfig(): Promise<AuthConfig> {
    return apiClient.get<AuthConfig>('/auth/config');
  },
};
