import { useState, useEffect, useCallback } from 'react';

const SIDEBAR_STATE_KEY = 'trivy-sidebar-collapsed';

export function useSidebarState() {
  const [isCollapsed, setIsCollapsed] = useState(() => {
    if (typeof window === 'undefined') return false;
    const stored = localStorage.getItem(SIDEBAR_STATE_KEY);
    return stored === 'true';
  });

  useEffect(() => {
    localStorage.setItem(SIDEBAR_STATE_KEY, String(isCollapsed));
  }, [isCollapsed]);

  const toggle = useCallback(() => {
    setIsCollapsed(prev => !prev);
  }, []);

  const collapse = useCallback(() => {
    setIsCollapsed(true);
  }, []);

  const expand = useCallback(() => {
    setIsCollapsed(false);
  }, []);

  return {
    isCollapsed,
    toggle,
    collapse,
    expand
  };
}
