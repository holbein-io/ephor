import { parseImageRef, formatImageTag, cn } from '../utils';
import { Tooltip, TooltipTrigger, TooltipContent } from './ui/tooltip';

interface ImageRefProps {
  /** One reference, or the comma-joined list the API returns for image_names. */
  references: string | string[];
  /** Hide the tag chip where the column is too narrow to earn it. */
  showTag?: boolean;
  className?: string;
}

function toList(references: string | string[]): string[] {
  const list = Array.isArray(references) ? references : references.split(', ');
  return [...new Set(list.map(r => r.trim()).filter(Boolean))];
}

/**
 * Renders a container image so the segment that identifies the service survives.
 * Image references are general-to-specific left to right, so the path prefix
 * absorbs the truncation and the name never shrinks below full length.
 */
export function ImageRef({ references, showTag = true, className }: ImageRefProps) {
  const list = toList(references);

  if (list.length === 0) {
    return <span className="font-mono text-text-tertiary">&mdash;</span>;
  }

  const [primary, ...rest] = list;
  const parsed = parseImageRef(primary);
  const prefix = parsed.namespace ? `${parsed.namespace}/` : '';
  const tag = parsed.tag ?? (parsed.digest ? parsed.digest : undefined);

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span className={cn('inline-flex items-center gap-1 min-w-0 max-w-full font-mono cursor-default', className)}>
          {prefix && (
            <span className="shrink-[999] min-w-0 truncate text-text-tertiary">{prefix}</span>
          )}
          <span className="shrink min-w-0 truncate font-medium text-text-secondary">{parsed.name}</span>
          {showTag && tag && (
            <span className="shrink-0 font-semibold text-accent bg-accent/10 px-1 py-0.5 rounded text-[0.85em]">
              {formatImageTag(tag)}
            </span>
          )}
          {rest.length > 0 && (
            <span className="shrink-0 text-text-tertiary">+{rest.length}</span>
          )}
        </span>
      </TooltipTrigger>
      <TooltipContent className="max-w-[min(90vw,42rem)]">
        <div className="flex flex-col gap-1 font-mono">
          {list.map(ref => (
            <span key={ref} className="break-all">{ref}</span>
          ))}
        </div>
      </TooltipContent>
    </Tooltip>
  );
}
