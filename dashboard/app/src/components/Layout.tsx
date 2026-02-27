import { Link, useLocation } from 'react-router-dom';
import {
  Shield,
  AlertTriangle,
  ClipboardCheck,
  Home,
  LogOut,
  User,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import { cn } from '../utils';
import { useUser } from '../contexts/UserContext';
import { useNavBadges } from '../hooks/useNavBadges';
import { useSidebarState } from '../hooks/useSidebarState';

interface LayoutProps {
  children: React.ReactNode;
}

type NavItem = {
  name: string;
  href: string;
  icon: typeof Home;
  badgeKey?: 'vulnerabilities' | 'criticalHigh' | 'escalations';
  badgeVariant?: 'default' | 'danger';
};

const navigation: NavItem[] = [
  { name: 'Dashboard', href: '/', icon: Home },
  { name: 'Vulnerabilities', href: '/vulnerabilities', icon: Shield, badgeKey: 'criticalHigh', badgeVariant: 'danger' },
  { name: 'Escalations', href: '/escalations', icon: AlertTriangle, badgeKey: 'escalations', badgeVariant: 'danger' },
  { name: 'Triage', href: '/triage', icon: ClipboardCheck },
];

export function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const { user, isAuthenticated, isLoading, logout } = useUser();
  const badges = useNavBadges();
  const { isCollapsed, toggle } = useSidebarState();

  return (
    <div className="min-h-screen bg-bg-primary">
      {/* Sidebar */}
      <div
        className={cn(
          'fixed inset-y-0 left-0 z-50 bg-bg-secondary border-r border-border transition-all duration-300 ease-in-out',
          isCollapsed ? 'w-16' : 'w-64'
        )}
      >
        {/* Logo section */}
        <div className="flex h-16 items-center justify-between border-b border-border px-3">
          <div className={cn(
            'flex items-center overflow-hidden transition-all duration-300',
            isCollapsed ? 'w-0 opacity-0' : 'w-auto opacity-100'
          )}>
            <Shield className="h-8 w-8 text-accent flex-shrink-0" />
            <h1 className="text-xl font-display font-bold text-text-primary ml-2 whitespace-nowrap">Ephor</h1>
          </div>
          {isCollapsed && (
            <Shield className="h-8 w-8 text-accent mx-auto" />
          )}
        </div>

        {/* Navigation */}
        <nav className="mt-6 px-2">
          <ul className="space-y-1">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href ||
                (item.href !== '/' && location.pathname.startsWith(item.href));

              const badgeCount = item.badgeKey ? badges[item.badgeKey] : 0;

              return (
                <li key={item.name}>
                  <Link
                    to={item.href}
                    className={cn(
                      'group flex items-center text-sm font-medium rounded-lg transition-colors relative',
                      isCollapsed ? 'justify-center px-2 py-2.5' : 'px-3 py-2',
                      isActive
                        ? 'bg-accent/10 text-accent'
                        : 'text-text-secondary hover:bg-bg-tertiary hover:text-text-primary'
                    )}
                    title={isCollapsed ? item.name : undefined}
                  >
                    <item.icon
                      className={cn(
                        'h-5 w-5 flex-shrink-0',
                        isCollapsed ? '' : 'mr-3',
                        isActive ? 'text-accent' : 'text-text-tertiary group-hover:text-text-secondary'
                      )}
                    />
                    {!isCollapsed && (
                      <>
                        <span className="flex-1">{item.name}</span>
                        {badgeCount > 0 && (
                          <span
                            className={cn(
                              'ml-2 px-2 py-0.5 text-xs font-semibold rounded-full',
                              item.badgeVariant === 'danger'
                                ? 'bg-danger/15 text-danger'
                                : 'bg-bg-tertiary text-text-secondary'
                            )}
                          >
                            {badgeCount > 99 ? '99+' : badgeCount}
                          </span>
                        )}
                      </>
                    )}
                    {/* Badge dot when collapsed */}
                    {isCollapsed && badgeCount > 0 && (
                      <span
                        className={cn(
                          'absolute top-1 right-1 w-2 h-2 rounded-full',
                          item.badgeVariant === 'danger' ? 'bg-danger' : 'bg-text-tertiary'
                        )}
                      />
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* Collapse toggle button */}
        <button
          onClick={toggle}
          className="absolute bottom-4 left-1/2 -translate-x-1/2 p-2 rounded-full bg-bg-tertiary hover:bg-border-hover text-text-secondary transition-colors"
          title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {isCollapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Main content */}
      <div
        className={cn(
          'transition-all duration-300 ease-in-out',
          isCollapsed ? 'pl-16' : 'pl-64'
        )}
      >
        <header className="bg-bg-secondary border-b border-border">
          <div className="px-6 py-4 flex items-center justify-between">
            <h2 className="text-2xl font-display font-bold text-text-primary">
              {navigation.find(item => {
                const isActive = location.pathname === item.href ||
                  (item.href !== '/' && location.pathname.startsWith(item.href));
                return isActive;
              })?.name || 'Dashboard'}
            </h2>

            {/* User info and logout */}
            <div className="flex items-center space-x-4">
              {isLoading ? (
                <div className="h-8 w-24 bg-bg-tertiary rounded animate-pulse"></div>
              ) : isAuthenticated && user ? (
                <>
                  <div className="flex items-center space-x-2 text-sm text-text-secondary">
                    <User className="h-4 w-4 text-text-tertiary" />
                    <span className="font-medium">{user.displayName || user.username}</span>
                  </div>
                  <button
                    onClick={logout}
                    className="flex items-center space-x-1 px-3 py-1.5 text-sm text-text-secondary hover:text-text-primary hover:bg-bg-tertiary rounded-lg transition-colors"
                    title="Sign out"
                  >
                    <LogOut className="h-4 w-4" />
                    <span>Logout</span>
                  </button>
                </>
              ) : (
                <span className="text-sm text-text-tertiary">Not logged in</span>
              )}
            </div>
          </div>
        </header>

        <main className="px-6 py-8">
          {children}
        </main>
      </div>
    </div>
  );
}
