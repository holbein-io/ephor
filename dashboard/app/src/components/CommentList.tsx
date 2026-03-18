import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2, Pencil } from 'lucide-react';
import { useState } from 'react';
import { commentsService, CommentDto } from '../services/api/comments.service';
import { useUser } from '../contexts/UserContext';
import { formatRelativeTime } from '../utils';
import { Button } from './ui/button';

interface CommentListProps {
  entityType: 'VULNERABILITY' | 'ESCALATION';
  entityId: number;
}

export function CommentList({ entityType, entityId }: CommentListProps) {
  const queryClient = useQueryClient();
  const { user, hasPermission } = useUser();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editBody, setEditBody] = useState('');

  const queryKey = ['comments', entityType, entityId];

  const fetchFn = entityType === 'VULNERABILITY'
    ? () => commentsService.getVulnerabilityComments(entityId)
    : () => commentsService.getEscalationComments(entityId);

  const { data: comments, isLoading } = useQuery({
    queryKey,
    queryFn: fetchFn,
    enabled: !!entityId,
  });

  const updateMutation = useMutation({
    mutationFn: ({ commentId, body }: { commentId: number; body: string }) =>
      commentsService.updateComment(commentId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setEditingId(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (commentId: number) => commentsService.deleteComment(commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const canModify = (comment: CommentDto) => {
    return user?.username === comment.createdBy || hasPermission('MANAGE_ADMIN');
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[...Array(2)].map((_, i) => (
          <div key={i} className="h-16 bg-bg-tertiary rounded animate-pulse"></div>
        ))}
      </div>
    );
  }

  if (!comments || comments.length === 0) {
    return (
      <p className="text-sm text-text-tertiary text-center py-4">No comments yet</p>
    );
  }

  return (
    <div className="space-y-3 max-h-64 overflow-y-auto">
      {comments.map((comment) => (
        <div key={comment.id} className="p-3 bg-bg-secondary rounded-lg">
          {editingId === comment.id ? (
            <div className="space-y-2">
              <textarea
                className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent"
                rows={3}
                value={editBody}
                onChange={(e) => setEditBody(e.target.value)}
                onKeyDown={(e) => {
                  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                    e.preventDefault();
                    if (editBody.trim()) {
                      updateMutation.mutate({ commentId: comment.id, body: editBody });
                    }
                  }
                  if (e.key === 'Escape') {
                    setEditingId(null);
                  }
                }}
              />
              <div className="flex space-x-2">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => updateMutation.mutate({ commentId: comment.id, body: editBody })}
                  disabled={!editBody.trim() || updateMutation.isPending}
                  loading={updateMutation.isPending}
                >
                  Save
                </Button>
                <Button variant="outline" size="sm" onClick={() => setEditingId(null)}>
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm font-medium text-text-primary">{comment.createdBy}</span>
                  <span className="text-xs text-text-tertiary">
                    {formatRelativeTime(comment.createdAt)}
                    {comment.updatedAt && comment.updatedAt !== comment.createdAt && ' (edited)'}
                  </span>
                </div>
                <p className="text-sm text-text-secondary whitespace-pre-wrap">{comment.body}</p>
              </div>
              {canModify(comment) && (
                <div className="flex gap-1 flex-shrink-0">
                  <button
                    onClick={() => {
                      setEditingId(comment.id);
                      setEditBody(comment.body);
                    }}
                    className="text-text-tertiary hover:text-text-primary p-1"
                    title="Edit comment"
                  >
                    <Pencil className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => {
                      if (confirm('Delete this comment?')) {
                        deleteMutation.mutate(comment.id);
                      }
                    }}
                    className="text-text-tertiary hover:text-danger p-1"
                    title="Delete comment"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
