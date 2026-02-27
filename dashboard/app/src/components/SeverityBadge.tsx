import { Badge } from './ui/badge';
import { getSeverityColor } from '../utils';

interface SeverityBadgeProps {
  severity: string;
  className?: string;
}

export function SeverityBadge({ severity, className }: SeverityBadgeProps) {
  return (
    <Badge className={`${getSeverityColor(severity)} ${className}`}>
      {severity}
    </Badge>
  );
}