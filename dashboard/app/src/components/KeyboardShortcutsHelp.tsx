import { useState } from 'react';
import { Keyboard, X } from 'lucide-react';
import { Button } from './ui/button';

interface ShortcutItem {
  key: string;
  description: string;
}

interface ShortcutGroup {
  title: string;
  shortcuts: ShortcutItem[];
}

interface KeyboardShortcutsHelpProps {
  groups: ShortcutGroup[];
}

export function KeyboardShortcutsHelp({ groups }: KeyboardShortcutsHelpProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      {/* Trigger button */}
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-4 right-4 p-3 bg-bg-tertiary text-text-primary rounded-full shadow-lg hover:bg-border-hover transition-colors z-40"
        title="Keyboard shortcuts (?)"
      >
        <Keyboard className="h-5 w-5" />
      </button>

      {/* Modal */}
      {isOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-bg-card rounded-xl shadow-2xl w-full max-w-md overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b bg-bg-secondary">
              <div className="flex items-center gap-3">
                <Keyboard className="h-5 w-5 text-text-secondary" />
                <h2 className="text-lg font-semibold text-text-primary">Keyboard Shortcuts</h2>
              </div>
              <Button variant="ghost" size="sm" onClick={() => setIsOpen(false)}>
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="px-6 py-4 max-h-[60vh] overflow-y-auto">
              {groups.map((group, idx) => (
                <div key={group.title} className={idx > 0 ? 'mt-6' : ''}>
                  <h3 className="text-xs font-semibold text-text-tertiary uppercase tracking-wider mb-3">
                    {group.title}
                  </h3>
                  <div className="space-y-2">
                    {group.shortcuts.map((shortcut) => (
                      <div
                        key={shortcut.key}
                        className="flex items-center justify-between py-1.5"
                      >
                        <span className="text-sm text-text-secondary">{shortcut.description}</span>
                        <kbd className="px-2 py-1 text-xs font-mono font-semibold text-text-primary bg-bg-tertiary border border-border rounded shadow-sm">
                          {shortcut.key}
                        </kbd>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            <div className="px-6 py-3 border-t bg-bg-secondary text-center">
              <p className="text-xs text-text-tertiary">
                Press <kbd className="px-1.5 py-0.5 text-xs font-mono bg-bg-tertiary rounded">?</kbd> to toggle this help
              </p>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// Simpler inline shortcut hint component
export function ShortcutHint({ shortcut }: { shortcut: string }) {
  return (
    <kbd className="ml-2 px-1.5 py-0.5 text-xs font-mono text-text-tertiary bg-bg-tertiary border border-border rounded">
      {shortcut}
    </kbd>
  );
}
