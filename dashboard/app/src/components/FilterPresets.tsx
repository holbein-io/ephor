import { useState } from 'react';
import { Bookmark, ChevronDown, Plus, Trash2, Star } from 'lucide-react';
import { VulnerabilityFilters } from '../types';
import { Button } from './ui/button';

interface FilterPreset {
  id: string;
  name: string;
  filters: Partial<VulnerabilityFilters>;
  isBuiltIn?: boolean;
}

// Built-in presets that are always available
const BUILT_IN_PRESETS: FilterPreset[] = [
  {
    id: 'critical-unfixed',
    name: 'Critical & High (Open)',
    filters: { severity: ['CRITICAL', 'HIGH'], status: ['open', 'triaged'] },
    isBuiltIn: true
  },
  {
    id: 'needs-triage',
    name: 'Needs Triage',
    filters: { status: ['open'], sort_by: 'first_detected', sort_order: 'asc' },
    isBuiltIn: true
  },
  {
    id: 'recently-detected',
    name: 'Recently Detected',
    filters: { status: ['open', 'triaged'], sort_by: 'first_detected', sort_order: 'desc' },
    isBuiltIn: true
  },
  {
    id: 'fix-available',
    name: 'Has Fix Available',
    filters: { status: ['open', 'triaged'] },
    isBuiltIn: true
  },
  {
    id: 'resolved',
    name: 'Resolved',
    filters: { status: ['resolved'] },
    isBuiltIn: true
  }
];

const STORAGE_KEY = 'ephor-filter-presets';

function loadCustomPresets(): FilterPreset[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
}

function saveCustomPresets(presets: FilterPreset[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(presets));
}

interface FilterPresetsProps {
  currentFilters: VulnerabilityFilters;
  onApplyPreset: (filters: Partial<VulnerabilityFilters>) => void;
}

export function FilterPresets({ currentFilters, onApplyPreset }: FilterPresetsProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [customPresets, setCustomPresets] = useState<FilterPreset[]>(loadCustomPresets);
  const [showSaveDialog, setShowSaveDialog] = useState(false);
  const [newPresetName, setNewPresetName] = useState('');

  const handleSavePreset = () => {
    if (!newPresetName.trim()) return;

    const newPreset: FilterPreset = {
      id: `custom-${Date.now()}`,
      name: newPresetName.trim(),
      filters: {
        severity: currentFilters.severity,
        status: currentFilters.status,
        namespace: currentFilters.namespace,
        scanner_type: currentFilters.scanner_type,
        search: currentFilters.search,
        sort_by: currentFilters.sort_by,
        sort_order: currentFilters.sort_order
      }
    };

    const updated = [...customPresets, newPreset];
    setCustomPresets(updated);
    saveCustomPresets(updated);
    setNewPresetName('');
    setShowSaveDialog(false);
  };

  const handleDeletePreset = (id: string) => {
    const updated = customPresets.filter(p => p.id !== id);
    setCustomPresets(updated);
    saveCustomPresets(updated);
  };

  const handleApplyPreset = (preset: FilterPreset) => {
    onApplyPreset(preset.filters);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      <Button
        variant="outline"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2"
      >
        <Bookmark className="h-4 w-4" />
        <span>Presets</span>
        <ChevronDown className={`h-4 w-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </Button>

      {isOpen && (
        <>
          {/* Backdrop */}
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />

          {/* Dropdown */}
          <div className="absolute top-full mt-1 left-0 z-50 w-64 bg-bg-card rounded-lg shadow-xl border border-border py-2">
            {/* Built-in presets */}
            <div className="px-3 py-1.5">
              <p className="text-xs font-semibold text-text-tertiary uppercase tracking-wider">Quick Filters</p>
            </div>
            {BUILT_IN_PRESETS.map((preset) => (
              <button
                key={preset.id}
                onClick={() => handleApplyPreset(preset)}
                className="w-full text-left px-3 py-2 text-sm text-text-secondary hover:bg-bg-tertiary flex items-center gap-2"
              >
                <Star className="h-3.5 w-3.5 text-warning" />
                {preset.name}
              </button>
            ))}

            {/* Custom presets */}
            {customPresets.length > 0 && (
              <>
                <div className="border-t border-border-subtle my-2" />
                <div className="px-3 py-1.5">
                  <p className="text-xs font-semibold text-text-tertiary uppercase tracking-wider">Saved Views</p>
                </div>
                {customPresets.map((preset) => (
                  <div
                    key={preset.id}
                    className="flex items-center gap-2 px-3 py-2 hover:bg-bg-tertiary group"
                  >
                    <button
                      onClick={() => handleApplyPreset(preset)}
                      className="flex-1 text-left text-sm text-text-secondary flex items-center gap-2"
                    >
                      <Bookmark className="h-3.5 w-3.5 text-accent" />
                      {preset.name}
                    </button>
                    <button
                      onClick={() => handleDeletePreset(preset.id)}
                      className="p-1 text-text-tertiary hover:text-danger opacity-0 group-hover:opacity-100 transition-opacity"
                      title="Delete preset"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                ))}
              </>
            )}

            {/* Save current filters */}
            <div className="border-t border-border-subtle mt-2 pt-2">
              {showSaveDialog ? (
                <div className="px-3 py-2">
                  <input
                    type="text"
                    value={newPresetName}
                    onChange={(e) => setNewPresetName(e.target.value)}
                    placeholder="Enter preset name..."
                    className="w-full px-2 py-1.5 text-sm border border-border rounded focus:outline-none focus:ring-2 focus:ring-accent"
                    autoFocus
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleSavePreset();
                      if (e.key === 'Escape') setShowSaveDialog(false);
                    }}
                  />
                  <div className="flex gap-2 mt-2">
                    <Button size="sm" variant="primary" onClick={handleSavePreset}>
                      Save
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setShowSaveDialog(false)}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => setShowSaveDialog(true)}
                  className="w-full text-left px-3 py-2 text-sm text-accent hover:bg-accent/10 flex items-center gap-2"
                >
                  <Plus className="h-3.5 w-3.5" />
                  Save current filters...
                </button>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
