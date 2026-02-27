import { Badge } from './ui/badge';
import { getStatusColor, formatStatus } from '../utils';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  return (
    <Badge className={`${getStatusColor(status)} ${className}`}>
      {formatStatus(status)}
    </Badge>
  );
}