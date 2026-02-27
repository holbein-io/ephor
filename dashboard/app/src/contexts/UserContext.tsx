import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { authService, User } from '../services/api/auth.service';

interface UserContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  displayName: string;
  logout: () => void;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

interface UserProviderProps {
  children: ReactNode;
}

export function UserProvider({ children }: UserProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchUser() {
      try {
        const response = await authService.getCurrentUser();
        if (response.authenticated && response.user) {
          setUser(response.user);
        }
      } catch (error) {
        console.error('Failed to fetch user:', error);
      } finally {
        setIsLoading(false);
      }
    }

    fetchUser();
  }, []);

  const logout = async () => {
    try {
      const config = await authService.getAuthConfig();
      // Use the logout URL from config - API will construct the proper URL
      // including any redirects needed for the IdP
      window.location.href = config.logoutUrl || '/oauth2/sign_out';
    } catch {
      window.location.href = '/oauth2/sign_out';
    }
  };

  const value: UserContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    displayName: user?.displayName || user?.username || '',
    logout,
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
