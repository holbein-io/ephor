import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { format, formatDistanceToNow, isValid, parseISO } from 'date-fns';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(date: string | Date): string {
  const parsedDate = typeof date === 'string' ? parseISO(date) : date;
  if (!isValid(parsedDate)) return 'Invalid date';
  return format(parsedDate, 'MMM d, yyyy HH:mm');
}

export function formatDateOnly(date: string | Date): string {
  const parsedDate = typeof date === 'string' ? parseISO(date) : date;
  if (!isValid(parsedDate)) return 'Invalid date';
  return format(parsedDate, 'MMM d, yyyy');
}

export function formatRelativeTime(date: string | Date): string {
  const parsedDate = typeof date === 'string' ? parseISO(date) : date;
  if (!isValid(parsedDate)) return 'Invalid date';
  return formatDistanceToNow(parsedDate, { addSuffix: true });
}

export function getSeverityColor(severity: string): string {
  switch (severity) {
    case 'CRITICAL':
      return 'text-severity-critical bg-severity-critical/15 border-severity-critical/30';
    case 'HIGH':
      return 'text-severity-high bg-severity-high/15 border-severity-high/30';
    case 'MEDIUM':
      return 'text-severity-medium bg-severity-medium/15 border-severity-medium/30';
    case 'LOW':
      return 'text-severity-low bg-severity-low/15 border-severity-low/30';
    default:
      return 'text-severity-unknown bg-severity-unknown/15 border-severity-unknown/30';
  }
}

export function getStatusColor(status: string): string {
  switch (status) {
    case 'open':
      return 'text-danger bg-danger/15 border-danger/30';
    case 'triaged':
      return 'text-accent bg-accent/15 border-accent/30';
    case 'resolved':
      return 'text-success bg-success/15 border-success/30';
    case 'false_positive':
      return 'text-severity-low bg-severity-low/15 border-severity-low/30';
    case 'accepted_risk':
      return 'text-warning bg-warning/15 border-warning/30';
    default:
      return 'text-text-secondary bg-bg-tertiary border-border';
  }
}

export function formatStatus(status: string): string {
  switch (status) {
    case 'false_positive':
      return 'False Positive';
    case 'accepted_risk':
      return 'Accepted Risk';
    default:
      return status.charAt(0).toUpperCase() + status.slice(1);
  }
}

export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
}

export function shortenImageName(fullName: string): string {
  const parts = fullName.split('/');
  if (parts.length >= 3) return parts.slice(-2).join('/');
  if (parts.length === 2 && parts[0].includes('.')) return parts[1];
  return fullName;
}

export function formatImageTag(tag: string): string {
  if (tag.length === 40) {
    return tag.substring(0, 8);
  } else if (tag.startsWith('sha256:')) {
    return 'sha:' + tag.substring(7, 15);
  }
  return tag;
}

export function pluralize(count: number, singular: string, plural?: string): string {
  if (count === 1) return `${count} ${singular}`;
  return `${count} ${plural || singular + 's'}`;
}

export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout>;
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

export function getVulnerabilityAge(firstDetected: string | Date): number {
  const parsedDate = typeof firstDetected === 'string' ? parseISO(firstDetected) : firstDetected;
  if (!isValid(parsedDate)) return 0;
  const now = new Date();
  const diffMs = now.getTime() - parsedDate.getTime();
  return Math.floor(diffMs / (1000 * 60 * 60 * 24));
}

export function getAgeBadgeInfo(days: number): {
  label: string;
  color: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
} {
  if (days >= 90) {
    return {
      label: `${days}d old`,
      color: 'bg-severity-critical/15 text-severity-critical border-severity-critical/30',
      severity: 'critical',
    };
  } else if (days >= 60) {
    return {
      label: `${days}d old`,
      color: 'bg-severity-high/15 text-severity-high border-severity-high/30',
      severity: 'high',
    };
  } else if (days >= 30) {
    return {
      label: `${days}d old`,
      color: 'bg-severity-medium/15 text-severity-medium border-severity-medium/30',
      severity: 'medium',
    };
  } else if (days >= 7) {
    return {
      label: `${days}d old`,
      color: 'bg-severity-low/15 text-severity-low border-severity-low/30',
      severity: 'low',
    };
  }
  return { label: 'New', color: 'bg-success/15 text-success border-success/30', severity: 'low' };
}
