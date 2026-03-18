import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { authService, User, Permission, AuthConfig } from '../services/api/auth.service';

interface UserContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  displayName: string;
  logout: () => void;
  hasPermission: (permission: Permission) => boolean;
  hasAnyPermission: (...permissions: Permission[]) => boolean;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

interface UserProviderProps {
  children: ReactNode;
}

export function UserProvider({ children }: UserProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [authConfig, setAuthConfig] = useState<AuthConfig | null>(null);

  useEffect(() => {
    async function init() {
      try {
        const [userResponse, config] = await Promise.all([
          authService.getCurrentUser(),
          authService.getAuthConfig(),
        ]);
        if (userResponse.authenticated && userResponse.user) {
          setUser(userResponse.user);
        }
        setAuthConfig(config);
      } catch (error) {
        console.error('Failed to fetch user:', error);
      } finally {
        setIsLoading(false);
      }
    }

    init();
  }, []);

  const logout = () => {
    const appUrl = `${window.location.protocol}//${window.location.host}`;

    if (authConfig?.idpLogoutUrl) {
      const idpLogout = `${authConfig.idpLogoutUrl}?post_logout_redirect_uri=${encodeURIComponent(appUrl)}&client_id=ephor-app`;
      window.location.href = `/oauth2/sign_out?rd=${encodeURIComponent(idpLogout)}`;
    } else {
      window.location.href = authConfig?.logoutUrl || '/oauth2/sign_out';
    }
  };

  const hasPermission = (permission: Permission): boolean => {
    return user?.permissions?.includes(permission) ?? false;
  };

  const hasAnyPermission = (...permissions: Permission[]): boolean => {
    return permissions.some(p => user?.permissions?.includes(p) ?? false);
  };

  const value: UserContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    displayName: user?.displayName || user?.username || '',
    logout,
    hasPermission,
    hasAnyPermission,
  };

  return (
    <UserContext.Provider value={value}>
      {children}
    </UserContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useUser(): UserContextType {
  const context = useContext(UserContext);
  if (context === undefined) {
    throw new Error('useUser must be used within a UserProvider');
  }
  return context;
}
