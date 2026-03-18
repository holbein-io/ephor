import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { usersService, DirectoryCapabilities } from '../services/api/users.service';

interface UserDirectoryContextType {
  capabilities: DirectoryCapabilities | null;
  isLoading: boolean;
}

const UserDirectoryContext = createContext<UserDirectoryContextType | undefined>(undefined);

export function UserDirectoryProvider({ children }: { children: ReactNode }) {
  const [capabilities, setCapabilities] = useState<DirectoryCapabilities | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    usersService.getCapabilities()
      .then(setCapabilities)
      .catch(() => setCapabilities(null))
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <UserDirectoryContext.Provider value={{ capabilities, isLoading }}>
      {children}
    </UserDirectoryContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useUserDirectory(): UserDirectoryContextType {
  const context = useContext(UserDirectoryContext);
  if (context === undefined) {
    throw new Error('useUserDirectory must be used within a UserDirectoryProvider');
  }
  return context;
}
