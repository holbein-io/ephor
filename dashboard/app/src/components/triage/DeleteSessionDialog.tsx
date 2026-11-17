import { AlertTriangle } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { Button } from '../ui/button';
import { TriageSession } from '../../types';
import { formatDateOnly } from '../../utils';

interface DeleteSessionDialogProps {
  session: TriageSession | null;
  isPending: boolean;
  error?: string | null;
  onConfirm: (session: TriageSession) => void;
  onClose: () => void;
}

export function DeleteSessionDialog({ session, isPending, error, onConfirm, onClose }: DeleteSessionDialogProps) {
  const decisions = session?.decisions_count || 0;
  const preparations = session?.preparations_count || 0;
  const bulkPlans = session?.bulk_plans_count || 0;

  return (
    <Dialog open={!!session} onOpenChange={(open) => { if (!open && !isPending) onClose(); }}>
      <DialogContent className="max-w-md" onInteractOutside={(e) => { if (isPending) e.preventDefault(); }}>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-danger" />
            Delete triage session?
          </DialogTitle>
          <DialogDescription>
            {session && (
              <>
                The session from <span className="text-text-primary font-medium">{formatDateOnly(session.session_date)}</span>
                {' '}will be permanently removed, together with its{' '}
                <span className="font-mono">{preparations}</span> prepared vulnerabilit{preparations === 1 ? 'y' : 'ies'},{' '}
                <span className="font-mono">{decisions}</span> decision{decisions === 1 ? '' : 's'} and{' '}
                <span className="font-mono">{bulkPlans}</span> bulk plan{bulkPlans === 1 ? '' : 's'}.
              </>
            )}
          </DialogDescription>
        </DialogHeader>

        {decisions > 0 && (
          <p className="text-sm text-text-secondary bg-bg-tertiary border border-border rounded-lg px-3 py-2">
            Vulnerability statuses set by these decisions and any remediations created from them are kept.
            The remediations will no longer reference this session.
          </p>
        )}

        {error && <p className="text-sm text-danger">{error}</p>}

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isPending}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={() => session && onConfirm(session)}
            disabled={isPending}
            loading={isPending}
          >
            Delete session
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
