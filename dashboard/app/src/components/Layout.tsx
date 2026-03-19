import { Link, useLocation } from 'react-router-dom';
import { Search, LogOut } from 'lucide-react';
import { cn } from '../utils';
import { useUser } from '../contexts/UserContext';
import { useUserDirectory } from '../contexts/UserDirectoryContext';
import { useNavBadges } from '../hooks/useNavBadges';

interface LayoutProps {
  children: React.ReactNode;
}

type NavItem = {
  name: string;
  href: string;
  badgeKey?: 'vulnerabilities' | 'criticalHigh' | 'escalations';
};

const navigation: NavItem[] = [
  { name: 'Dashboard', href: '/' },
  { name: 'Vulnerabilities', href: '/vulnerabilities', badgeKey: 'criticalHigh' },
  { name: 'Triage', href: '/triage' },
  { name: 'Escalations', href: '/escalations', badgeKey: 'escalations' },
];

export function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const { user, isAuthenticated, logout } = useUser();
  const { capabilities } = useUserDirectory();
  const badges = useNavBadges();

  const dynamicNav: NavItem[] = capabilities?.my_items_enabled
    ? [{ name: 'My Items', href: '/my-items' }]
    : [];
  const allNavigation = [...navigation, ...dynamicNav];

  const initials = user?.displayName
    ? user.displayName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.username?.slice(0, 2).toUpperCase() || '?';

  return (
    <div className="min-h-screen bg-bg-primary">
      {/* Top Navigation */}
      <nav className="sticky top-0 z-50 flex items-center h-14 px-6 bg-bg-secondary border-b border-border animate-slide-in">
        {/* Brand */}
        <Link to="/" className="flex items-baseline gap-2 mr-10 no-underline">
          <span className="font-display text-xl italic text-text-primary">Ephor</span>
          <span className="font-mono text-[10px] text-text-tertiary bg-bg-tertiary border border-border-subtle px-1.5 py-0.5 rounded">
            v2.4
          </span>
        </Link>

        {/* Nav Links */}
        <div className="flex items-center gap-0.5 flex-1">
          {allNavigation.map((item) => {
            const isActive = location.pathname === item.href ||
              (item.href !== '/' && location.pathname.startsWith(item.href));

            const badgeCount = item.badgeKey ? badges[item.badgeKey] : 0;

            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'relative px-3 py-1.5 rounded-lg text-[13.5px] font-medium transition-all duration-150',
                  isActive
                    ? 'text-accent bg-accent-dim'
                    : 'text-text-secondary hover:text-text-primary hover:bg-bg-hover'
                )}
              >
                {item.name}
                {badgeCount > 0 && (
                  <span className="absolute top-1 right-1.5 w-1.5 h-1.5 rounded-full bg-severity-critical animate-[pulse_2s_infinite]" />
                )}
              </Link>
            );
          })}
        </div>

        {/* Right section */}
        <div className="flex items-center gap-3 ml-auto">
          {/* Search */}
          <div className="flex items-center gap-2 bg-bg-tertiary border border-border rounded-lg px-3 py-1.5 w-[200px] focus-within:border-accent focus-within:w-[280px] transition-all duration-200">
            <Search className="h-3.5 w-3.5 text-text-tertiary flex-shrink-0" />
            <input
              type="text"
              placeholder="Search..."
              className="bg-transparent border-none outline-none font-body text-[13px] text-text-primary w-full placeholder:text-text-tertiary"
            />
            <kbd className="font-mono text-[10px] text-text-tertiary bg-bg-hover px-1.5 py-0.5 rounded border border-border ml-auto flex-shrink-0">
              /
            </kbd>
          </div>

          {/* Avatar / User */}
          {isAuthenticated && user ? (
            <div className="flex items-center gap-2">
              <button
                onClick={logout}
                className="p-1.5 rounded-lg text-text-tertiary hover:text-text-secondary hover:bg-bg-hover transition-colors"
                title="Sign out"
              >
                <LogOut className="h-4 w-4" />
              </button>
              <div
                className="w-8 h-8 rounded-[10px] flex items-center justify-center text-xs font-bold text-white cursor-pointer"
                style={{ background: 'linear-gradient(135deg, #E8613A, #C04A2A)' }}
                title={user.displayName || user.username}
              >
                {initials}
              </div>
            </div>
          ) : (
            <div
              className="w-8 h-8 rounded-[10px] bg-bg-tertiary flex items-center justify-center text-xs text-text-tertiary"
            >
              ?
            </div>
          )}
        </div>
      </nav>

      {/* Main content */}
      <main className="px-6 py-7">
        {children}
      </main>
    </div>
  );
}
