import axios, { AxiosInstance, AxiosError } from 'axios';

/**
 * Base API client configuration
 * All services will use this configured axios instance
 */
class ApiClient {
  private instance: AxiosInstance;

  constructor() {
    this.instance = axios.create({
      baseURL: '/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
      paramsSerializer: {
        indexes: null, // Ensures arrays are serialized as param=val1&param=val2
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor for auth token (if needed in future)
    this.instance.interceptors.request.use(
      (config) => {
        // Add auth token if available
        const token = localStorage.getItem('authToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.instance.interceptors.response.use(
      (response) => {
        if (import.meta.env.DEV) {
          console.log(`[API] ${response.config.method?.toUpperCase()} ${response.config.url}`, response.data);
        }
        return response.data;
      },
      (error: AxiosError) => {
        // Centralized error handling
        if (error.response) {
          // Server responded with error status
          const message = (error.response.data as any)?.message || error.message;

          switch (error.response.status) {
            case 401:
              // Unauthorized - redirect to login if needed
              console.error('Unauthorized access');
              break;
            case 404:
              console.error('Resource not found');
              break;
            case 500:
              console.error('Server error:', message);
              break;
            default:
              console.error('API error:', message);
          }
        } else if (error.request) {
          // Request was made but no response
          console.error('Network error - no response received');
        } else {
          // Error in request setup
          console.error('Request error:', error.message);
        }

        return Promise.reject(error);
      }
    );
  }

  get<T = any>(url: string, params?: any): Promise<T> {
    return this.instance.get(url, { params });
  }

  post<T = any>(url: string, data?: any): Promise<T> {
    return this.instance.post(url, data);
  }

  put<T = any>(url: string, data?: any): Promise<T> {
    return this.instance.put(url, data);
  }

  patch<T = any>(url: string, data?: any): Promise<T> {
    return this.instance.patch(url, data);
  }

  delete<T = any>(url: string): Promise<T> {
    return this.instance.delete(url);
  }
}

// Export singleton instance
export const apiClient = new ApiClient();