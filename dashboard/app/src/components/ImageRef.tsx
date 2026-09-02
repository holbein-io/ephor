import { imageRefDisplay, formatImageTag, cn } from '../utils';
import { Tooltip, TooltipTrigger, TooltipContent } from './ui/tooltip';

interface ImageRefProps {
  /** One reference, or the comma-joined list the API returns for image_names. */
  references: string | string[];
  showTag?: boolean;
  className?: string;
}

function toList(references: string | string[]): string[] {
  const list = Array.isArray(references) ? references : references.split(', ');
  return [...new Set(list.map(r => r.trim()).filter(Boolean))];
}

/**
 * Renders a container image so the segment that identifies the service survives.
 * The lead absorbs the truncation from its start, so a reverse-DNS repository
 * keeps the qualifier next to the name rather than the organisation prefix.
 */
export function ImageRef({ references, showTag = true, className }: ImageRefProps) {
  const list = toList(references);

  if (list.length === 0) {
    return <span className="font-mono text-text-tertiary">&mdash;</span>;
  }

  const [primary, ...rest] = list;
  const { lead, name, tag } = imageRefDisplay(primary);
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span className={cn('flex items-baseline gap-1 min-w-0 max-w-full font-mono cursor-default', className)}>
          {/* Shrink order: lead, then tag, then name. */}
          {lead && (
            <span className="clip-start shrink-[999] min-w-0 text-text-tertiary">{lead}</span>
          )}
          <span className="shrink min-w-0 truncate font-medium text-text-secondary">{name}</span>
          {showTag && tag && (
            <span className="shrink-[50] min-w-0 truncate font-semibold text-accent bg-accent/10 px-1 rounded text-[0.85em]">
              {formatImageTag(tag)}
            </span>
          )}
          {rest.length > 0 && (
            <span className="shrink-0 text-text-tertiary">+{rest.length}</span>
          )}
        </span>
      </TooltipTrigger>
      <TooltipContent className="max-w-[min(90vw,48rem)]">
        <div className="flex flex-col gap-1 font-mono">
          {list.map(ref => (
            <span key={ref} className="break-all">{ref}</span>
          ))}
        </div>
      </TooltipContent>
    </Tooltip>
  );
}
