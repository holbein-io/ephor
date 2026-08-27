import { type ClassValue, clsx } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';
import { format, formatDistanceToNow, isValid, parseISO } from 'date-fns';

// Without this, tailwind-merge reads the two sub-xs scale steps as text colours
// and drops the colour class they are combined with.
const twMerge = extendTailwindMerge({
  extend: {
    classGroups: {
      'font-size': [{ text: ['micro', 'caption'] }],
    },
  },
});

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

export interface ParsedImageRef {
  /** Registry host, including port when present. Undefined for implicit Docker Hub. */
  registry?: string;
  /** Path after the registry, e.g. "docker-local/platform/payments-api". */
  repository: string;
  /** Last path segment -- the part that identifies the service. */
  name: string;
  /** Second-to-last path segment, when there is one. */
  namespace?: string;
  tag?: string;
  /** Full digest including algorithm, e.g. "sha256:abc123...". */
  digest?: string;
  /** The reference exactly as received. */
  full: string;
}

const COLON_DIGEST = /:(sha\d{3}:[0-9a-f]+)$/i;

/**
 * The API composes this field as CONCAT(image_name, ':', image_tag), so a naive
 * split(':') loses the tag whenever the registry carries a port. Split on the last
 * colon that follows the last slash instead, and peel the digest off first.
 */
export function parseImageRef(reference: string): ParsedImageRef {
  const full = reference.trim();

  let remainder = full;
  let digest: string | undefined;
  const digestAt = remainder.indexOf('@');
  if (digestAt !== -1) {
    digest = remainder.slice(digestAt + 1) || undefined;
    remainder = remainder.slice(0, digestAt);
  } else {
    // Some collectors compose "repo:tag" with the digest, so the value can end
    // ":sha256:..." rather than "@sha256:". The algorithm prefix disambiguates.
    const colonDigest = remainder.match(COLON_DIGEST);
    if (colonDigest) {
      digest = colonDigest[1];
      remainder = remainder.slice(0, colonDigest.index);
    }
  }

  let tag: string | undefined;
  const lastColon = remainder.lastIndexOf(':');
  if (lastColon > remainder.lastIndexOf('/')) {
    tag = remainder.slice(lastColon + 1) || undefined;
    remainder = remainder.slice(0, lastColon);
  }

  const segments = remainder.split('/').filter(Boolean);

  let registry: string | undefined;
  const first = segments[0];
  if (segments.length > 1 && first && (first.includes('.') || first.includes(':') || first === 'localhost')) {
    registry = segments.shift();
  }

  const repository = segments.join('/') || remainder;
  const name = segments[segments.length - 1] || repository;
  const namespace = segments.length > 1 ? segments[segments.length - 2] : undefined;

  return { registry, repository, name, namespace, tag, digest, full };
}

const SHA1_HEX = /^[0-9a-f]{40}$/i;

export function formatImageTag(tag: string): string {
  if (tag.startsWith('sha256:')) {
    return 'sha:' + tag.substring(7, 15);
  }
  if (SHA1_HEX.test(tag)) {
    return tag.substring(0, 8);
  }
  // Version at the front, commit at the back; the timestamp between them drops.
  return middleEllipsis(tag, 18);
}

export function middleEllipsis(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  const head = Math.ceil((maxLength - 1) / 2);
  const tail = Math.floor((maxLength - 1) / 2);
  return `${text.slice(0, head)}…${text.slice(text.length - tail)}`;
}

export interface ImageRefDisplay {
  /** Path and package qualifiers. Clipped from the start, never the end. */
  lead: string;
  /** The segment that names the service. */
  name: string;
  tag?: string;
}

const normalize = (segment: string) => segment.replace(/[^a-z0-9]/gi, '').toLowerCase();

/**
 * Splits a reference into the service name and the qualifiers around it.
 * Corporate registries hold reverse-DNS repositories where the service is the
 * last dot-segment but the segment telling it apart from its siblings sits
 * further left, so the qualifiers become a lead that clips from its start.
 */
export function imageRefDisplay(reference: string): ImageRefDisplay {
  const parsed = parseImageRef(reference);

  const pathParts = parsed.repository.split('/').filter(Boolean);
  const dotParts = (pathParts.pop() ?? '').split('.').filter(Boolean);
  const name = dotParts.pop() ?? parsed.name;

  const pathSeen = new Set(pathParts.map(normalize));
  const normalizedName = normalize(name);

  const qualifiers = dotParts.filter(segment => {
    const key = normalize(segment);
    if (key.length < 4) return true;
    // A qualifier the repository or the name already states earns no width.
    return !pathSeen.has(key) && !normalizedName.startsWith(key);
  });

  const leadPath = pathParts.length ? `${pathParts.join('/')}/` : '';
  const leadQualifier = qualifiers.length ? `${qualifiers.join('.')}.` : '';

  return { lead: leadPath + leadQualifier, name, tag: parsed.tag ?? parsed.digest };
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
