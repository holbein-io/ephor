import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TriageSessionManager } from './TriageSessionManager';
import { DeleteSessionDialog } from './DeleteSessionDialog';
import { TriageSession } from '../../types';

const session: TriageSession = {
  id: 7,
  session_date: '2026-11-10',
  status: 'COMPLETED',
  preparations_count: 4,
  decisions_count: 3,
  bulk_plans_count: 1,
  created_at: '2026-11-10T09:00:00Z',
};

describe('TriageSessionManager', () => {
  it('asks to delete a session without selecting it', () => {
    const onSelectSession = vi.fn();
    const onDeleteSession = vi.fn();
    render(
      <TriageSessionManager
        sessions={[session]}
        currentSession={null}
        isLoading={false}
        onSelectSession={onSelectSession}
        onCreateSession={vi.fn()}
        onDeleteSession={onDeleteSession}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Delete session' }));

    expect(onDeleteSession).toHaveBeenCalledWith(session);
    expect(onSelectSession).not.toHaveBeenCalled();
  });
});

describe('DeleteSessionDialog', () => {
  it('confirms deletion with the session counts', () => {
    const onConfirm = vi.fn();
    render(
      <DeleteSessionDialog
        session={session}
        isPending={false}
        onConfirm={onConfirm}
        onClose={vi.fn()}
      />
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent(/4 prepared vulnerabilities, 3 decisions and 1 bulk plan\./);
    expect(dialog).toHaveTextContent(/remediations created from them are kept/);

    fireEvent.click(screen.getByRole('button', { name: 'Delete session' }));
    expect(onConfirm).toHaveBeenCalledWith(session);
  });

  it('renders nothing when no session is targeted', () => {
    render(<DeleteSessionDialog session={null} isPending={false} onConfirm={vi.fn()} onClose={vi.fn()} />);
    expect(screen.queryByText('Delete triage session?')).not.toBeInTheDocument();
  });
});
