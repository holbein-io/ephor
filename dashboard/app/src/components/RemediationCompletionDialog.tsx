import { useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { NativeSelect } from './ui/native-select';
import { X } from 'lucide-react';

const completionMethodOptions = [
  { value: 'manual', label: 'Manual' },
  { value: 'version_upgrade', label: 'Version Upgrade' }
];

const vulnStatusOptions = [
  { value: '', label: 'No change' },
  { value: 'resolved', label: 'Resolved' },
  { value: 'accepted_risk', label: 'Accepted Risk' },
  { value: 'false_positive', label: 'False Positive' }
];

interface RemediationCompletionDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (data: {
    completionMethod: 'manual' | 'version_upgrade';
    completedBy: string;
    vulnStatusUpdate?: 'resolved' | 'accepted_risk' | 'false_positive';
  }) => void;
  defaultCompletedBy: string;
  isPending: boolean;
}

export function RemediationCompletionDialog({
  open,
  onClose,
  onConfirm,
  defaultCompletedBy,
  isPending
}: RemediationCompletionDialogProps) {
  const [completionMethod, setCompletionMethod] = useState<'manual' | 'version_upgrade'>('manual');
  const [completedBy, setCompletedBy] = useState(defaultCompletedBy);
  const [vulnStatus, setVulnStatus] = useState('');

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-bg-card rounded-lg p-6 w-full max-w-md">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold">Complete Remediation</h3>
          <Button variant="ghost" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
        <div className="space-y-4">
          <NativeSelect
            label="Completion Method"
            options={completionMethodOptions}
            value={completionMethod}
            onChange={(e) => setCompletionMethod(e.target.value as 'manual' | 'version_upgrade')}
          />

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Completed By</label>
            <Input
              value={completedBy}
              onChange={(e) => setCompletedBy(e.target.value)}
              placeholder="Your name"
            />
          </div>

          <div className="border-t pt-4">
            <NativeSelect
              label="Also update vulnerability status?"
              options={vulnStatusOptions}
              value={vulnStatus}
              onChange={(e) => setVulnStatus(e.target.value)}
            />
            <p className="text-xs text-text-tertiary mt-1">
              Optionally set the vulnerability status alongside completing the remediation.
            </p>
          </div>

          <div className="flex space-x-2">
            <Button
              variant="primary"
              onClick={() => onConfirm({
                completionMethod,
                completedBy,
                vulnStatusUpdate: vulnStatus ? vulnStatus as 'resolved' | 'accepted_risk' | 'false_positive' : undefined
              })}
              disabled={!completedBy || isPending}
              loading={isPending}
            >
              Complete Remediation
            </Button>
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
