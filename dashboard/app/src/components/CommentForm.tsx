import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { commentsService } from '../services/api/comments.service';
import { Button } from './ui/button';

interface CommentFormProps {
  entityType: 'VULNERABILITY' | 'ESCALATION';
  entityId: number;
}

export function CommentForm({ entityType, entityId }: CommentFormProps) {
  const queryClient = useQueryClient();
  const [body, setBody] = useState('');

  const queryKey = ['comments', entityType, entityId];

  const addFn = entityType === 'VULNERABILITY'
    ? (text: string) => commentsService.addVulnerabilityComment(entityId, text)
    : (text: string) => commentsService.addEscalationComment(entityId, text);

  const mutation = useMutation({
    mutationFn: addFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setBody('');
    },
  });

  const handleSubmit = () => {
    if (body.trim()) {
      mutation.mutate(body.trim());
    }
  };

  return (
    <div className="flex gap-2">
      <textarea
        className="flex-1 px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent"
        rows={2}
        placeholder="Add a comment... (Ctrl+Enter to submit)"
        value={body}
        onChange={(e) => setBody(e.target.value)}
        onKeyDown={(e) => {
          if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            e.preventDefault();
            handleSubmit();
          }
        }}
      />
      <Button
        variant="primary"
        onClick={handleSubmit}
        disabled={!body.trim() || mutation.isPending}
        loading={mutation.isPending}
        className="self-end"
      >
        <Send className="h-4 w-4" />
      </Button>
    </div>
  );
}
