import { useState } from 'react';
import { ChevronDown, ChevronUp, Package, ExternalLink, Server } from 'lucide-react';
import { AffectedWorkload } from '../../types';
import { Badge } from '../ui/badge';

interface AffectedWorkloadsProps {
  workloads: AffectedWorkload[];
  packageName: string;
  packageVersion?: string;
  fixedVersion?: string;
  applyToAll: boolean;
  onApplyToAllChange: (value: boolean) => void;
}

export function AffectedWorkloads({
  workloads,
  packageName,
  packageVersion,
  fixedVersion,
  applyToAll,
  onApplyToAllChange
}: AffectedWorkloadsProps) {
  const [isExpanded, setIsExpanded] = useState(true);

  if (!workloads || workloads.length === 0) {
    return (
      <div className="text-sm text-text-tertiary italic py-2">
        No open workload instances found for this vulnerability.
      </div>
    );
  }

  return (
    <div className="border rounded-lg overflow-hidden">
      {/* Header */}
      <div
        className="flex items-center justify-between px-3 py-2 bg-bg-secondary cursor-pointer hover:bg-bg-tertiary"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="flex items-center gap-2">
          <Server className="w-4 h-4 text-text-tertiary" />
          <span className="text-sm font-medium text-text-secondary">
            Affected Workloads ({workloads.length})
          </span>
        </div>
        <div className="flex items-center gap-3">
          <label
            className="flex items-center gap-2 text-sm"
            onClick={(e) => e.stopPropagation()}
          >
            <input
              type="checkbox"
              checked={applyToAll}
              onChange={(e) => onApplyToAllChange(e.target.checked)}
              className="rounded border-border text-accent focus:ring-accent"
            />
            <span className="text-text-secondary">Apply to all</span>
          </label>
          {isExpanded ? (
            <ChevronUp className="w-4 h-4 text-text-tertiary" />
          ) : (
            <ChevronDown className="w-4 h-4 text-text-tertiary" />
          )}
        </div>
      </div>

      {/* Workload List */}
      {isExpanded && (
        <div className="divide-y divide-gray-100">
          {workloads.map((workload) => (
            <div
              key={workload.instance_id}
              className="px-3 py-2 hover:bg-bg-tertiary flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <Package className="w-4 h-4 text-accent" />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-text-primary">
                      {workload.name}
                    </span>
                    <Badge variant="default" className="text-xs">
                      {workload.namespace}
                    </Badge>
                    <span className="text-xs text-text-tertiary">
                      {workload.kind}
                    </span>
                  </div>
                  <div className="text-xs text-text-tertiary mt-0.5 flex items-center gap-2">
                    <span>
                      {packageName}
                      {packageVersion && `@${packageVersion}`}
                    </span>
                    {fixedVersion && (
                      <>
                        <span className="text-border">→</span>
                        <span className="text-success font-medium">
                          {fixedVersion} available
                        </span>
                      </>
                    )}
                  </div>
                </div>
              </div>
              <a
                href={`/vulnerabilities?workload=${workload.id}`}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-bg-tertiary text-text-secondary hover:bg-bg-tertiary rounded"
                onClick={(e) => e.stopPropagation()}
              >
                <ExternalLink className="w-3 h-3" />
                Details
              </a>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
