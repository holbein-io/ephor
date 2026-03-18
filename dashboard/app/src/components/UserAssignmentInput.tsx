import { useEffect, useRef, useState } from 'react';
import { useUserDirectory } from '../contexts/UserDirectoryContext';
import { usersService, KnownUserDto } from '../services/api/users.service';

interface UserAssignmentInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

export function UserAssignmentInput({
  value,
  onChange,
  placeholder,
  disabled,
}: UserAssignmentInputProps) {
  const { capabilities } = useUserDirectory();
  const [suggestions, setSuggestions] = useState<KnownUserDto[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Plain text input when search is not available
  if (!capabilities?.user_search_enabled) {
    return (
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className="w-full px-3 py-2 border border-border bg-bg-secondary text-text-primary rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent/50"
      />
    );
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const query = e.target.value;
    onChange(query);

    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (query.trim().length < 2) {
      setSuggestions([]);
      setShowDropdown(false);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      setIsSearching(true);
      try {
        const results = await usersService.searchUsers(query);
        setSuggestions(results);
        setShowDropdown(results.length > 0);
      } catch {
        setSuggestions([]);
        setShowDropdown(false);
      } finally {
        setIsSearching(false);
      }
    }, 300);
  }

  function handleSelect(user: KnownUserDto) {
    onChange(user.display_name || user.username);
    setShowDropdown(false);
    setSuggestions([]);
  }

  return (
    <div ref={containerRef} className="relative">
      <input
        type="text"
        value={value}
        onChange={handleInputChange}
        onFocus={() => suggestions.length > 0 && setShowDropdown(true)}
        placeholder={placeholder}
        disabled={disabled}
        className="w-full px-3 py-2 border border-border bg-bg-secondary text-text-primary rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent/50"
        autoComplete="off"
      />
      {isSearching && (
        <div className="absolute right-3 top-1/2 -translate-y-1/2">
          <div className="w-3 h-3 border border-text-tertiary border-t-accent rounded-full animate-spin" />
        </div>
      )}
      {showDropdown && suggestions.length > 0 && (
        <ul className="absolute z-10 mt-1 w-full bg-bg-card border border-border rounded-lg shadow-lg max-h-48 overflow-y-auto">
          {suggestions.map((user) => (
            <li
              key={user.username}
              onMouseDown={() => handleSelect(user)}
              className="px-3 py-2 cursor-pointer hover:bg-bg-tertiary transition-colors"
            >
              <span className="text-sm font-medium text-text-primary">{user.display_name || user.username}</span>
              {user.display_name && (
                <span className="text-xs text-text-tertiary ml-2">{user.username}</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
