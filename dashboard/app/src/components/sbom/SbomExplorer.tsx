import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { FileText, Download, ChevronRight, GitCompareArrows } from 'lucide-react';
import { sbomService } from '../../services/api';
import { SbomHistoryEntry, SbomDiffResult } from '../../types';
import { cn, formatDate } from '../../utils';

export function SbomExplorer() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [diffSelection, setDiffSelection] = useState<{ a: string | null; b: string | null }>({ a: null, b: null });

  const { data: images, isLoading } = useQuery({
    queryKey: ['sbom-images'],
    queryFn: () => sbomService.listImages(),
  });

  const { data: history } = useQuery({
    queryKey: ['sbom-history', selectedImage],
    queryFn: () => sbomService.getHistory(selectedImage!),
    enabled: !!selectedImage,
  });

  const { data: metadata } = useQuery({
    queryKey: ['sbom-metadata', selectedImage],
    queryFn: () => sbomService.getMetadata(selectedImage!),
    enabled: !!selectedImage,
  });

  const canDiff = diffSelection.a && diffSelection.b && diffSelection.a !== diffSelection.b;

  const { data: diffResult, isLoading: isDiffing } = useQuery({
    queryKey: ['sbom-diff', diffSelection.a, diffSelection.b],
    queryFn: () => sbomService.getDiff(diffSelection.a!, diffSelection.b!),
    enabled: !!canDiff,
  });

  if (isLoading) {
    return (
      <div className="px-6 py-12 text-center">
        <div className="inline-block h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!images?.length) {
    return (
      <div className="px-6 py-12 text-center">
        <FileText className="h-8 w-8 text-text-tertiary mx-auto mb-3" />
        <p className="text-sm text-text-tertiary">No SBOMs ingested yet.</p>
      </div>
    );
  }

  function handleDiffSelect(entry: SbomHistoryEntry) {
    setDiffSelection(prev => {
      if (!prev.a) return { a: entry.id, b: null };
      if (prev.a === entry.id) return { a: null, b: null };
      if (!prev.b) return { a: prev.a, b: entry.id };
      return { a: entry.id, b: null };
    });
  }

  return (
    <div className="grid grid-cols-12 gap-4">
      <div className="col-span-4 bg-bg-secondary border border-border rounded-2xl overflow-hidden">
        <div className="px-5 py-3 border-b border-border">
          <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
            Images ({images.length})
          </span>
        </div>
        <div className="max-h-[500px] overflow-y-auto">
          {images.map((img) => (
            <button
              key={img}
              onClick={() => { setSelectedImage(img); setDiffSelection({ a: null, b: null }); }}
              className={cn(
                'w-full text-left px-5 py-3 border-b border-border/50 last:border-b-0 transition-colors flex items-center justify-between',
                selectedImage === img ? 'bg-accent/5' : 'hover:bg-bg-hover'
              )}
            >
              <span className="font-mono text-[12px] text-text-primary truncate">{img}</span>
              <ChevronRight className={cn('h-3.5 w-3.5 text-text-tertiary flex-shrink-0 transition-transform',
                selectedImage === img && 'rotate-90'
              )} />
            </button>
          ))}
        </div>
      </div>

      <div className="col-span-8 space-y-4">
        {selectedImage && metadata && (
          <div className="bg-bg-secondary border border-border rounded-2xl px-5 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-mono text-[14px] font-medium text-text-primary">{selectedImage}</h3>
                <p className="text-[12px] text-text-secondary mt-1">
                  {metadata.format.toUpperCase()} -- {metadata.package_count} packages -- last seen {formatDate(metadata.last_seen)}
                </p>
              </div>
              <a
                href={sbomService.getDownloadUrl(selectedImage)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-bg-tertiary text-text-secondary border border-border hover:border-accent hover:text-accent transition-colors"
              >
                <Download className="h-3.5 w-3.5" />
                Download
              </a>
            </div>
          </div>
        )}

        {selectedImage && history && history.length > 0 && (
          <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden">
            <div className="px-5 py-3 border-b border-border flex items-center justify-between">
              <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
                Version History ({history.length})
              </span>
              {canDiff && (
                <span className="text-[11px] text-accent font-medium">
                  {isDiffing ? 'Comparing...' : 'Comparing 2 versions'}
                </span>
              )}
              {!canDiff && history.length > 1 && (
                <span className="text-[11px] text-text-tertiary">Select 2 versions to compare</span>
              )}
            </div>
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-5 py-2.5 w-10"></th>
                  <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Hash</th>
                  <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Format</th>
                  <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">First Seen</th>
                  <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Last Seen</th>
                </tr>
              </thead>
              <tbody>
                {history.map((entry) => {
                  const isSelected = entry.id === diffSelection.a || entry.id === diffSelection.b;
                  const label = entry.id === diffSelection.a ? 'A' : entry.id === diffSelection.b ? 'B' : null;
                  return (
                    <tr
                      key={entry.id}
                      onClick={() => handleDiffSelect(entry)}
                      className={cn(
                        'border-b border-border/50 last:border-b-0 cursor-pointer transition-colors',
                        isSelected ? 'bg-accent/5' : 'hover:bg-bg-hover'
                      )}
                    >
                      <td className="px-5 py-2.5 text-center">
                        {label && (
                          <span className="inline-flex items-center justify-center w-5 h-5 rounded-full text-[10px] font-bold bg-accent text-white">
                            {label}
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-2.5 font-mono text-[11px] text-text-secondary">
                        {entry.content_hash.substring(0, 12)}
                      </td>
                      <td className="px-5 py-2.5">
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-bg-tertiary text-text-secondary border border-border">
                          {entry.format}
                        </span>
                      </td>
                      <td className="px-5 py-2.5 text-[12px] text-text-secondary">{formatDate(entry.first_seen)}</td>
                      <td className="px-5 py-2.5 text-[12px] text-text-secondary">{formatDate(entry.last_seen)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {canDiff && diffResult && <DiffView diff={diffResult} />}
      </div>
    </div>
  );
}

function DiffView({ diff }: { diff: SbomDiffResult }) {
  const total = diff.added.length + diff.removed.length + diff.changed.length + diff.unchanged_count;

  return (
    <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden animate-fade-up">
      <div className="px-5 py-3 border-b border-border flex items-center gap-3">
        <GitCompareArrows className="h-4 w-4 text-accent" />
        <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
          Diff Result
        </span>
        <div className="flex gap-3 ml-auto text-[11px]">
          {diff.added.length > 0 && (
            <span className="text-accent-mint font-medium">+{diff.added.length} added</span>
          )}
          {diff.removed.length > 0 && (
            <span className="text-severity-critical font-medium">-{diff.removed.length} removed</span>
          )}
          {diff.changed.length > 0 && (
            <span className="text-severity-medium font-medium">~{diff.changed.length} changed</span>
          )}
          <span className="text-text-tertiary">{diff.unchanged_count} unchanged</span>
        </div>
      </div>

      {(diff.added.length > 0 || diff.removed.length > 0 || diff.changed.length > 0) ? (
        <div className="divide-y divide-border/50">
          {diff.added.map((pkg) => (
            <div key={`add-${pkg.name}`} className="flex items-center gap-3 px-5 py-2.5 bg-accent-mint/5">
              <span className="text-[10px] font-bold text-accent-mint w-4">+</span>
              <span className="font-mono text-[12px] text-text-primary">{pkg.name}</span>
              <span className="font-mono text-[11px] text-text-secondary">{pkg.version}</span>
            </div>
          ))}
          {diff.removed.map((pkg) => (
            <div key={`rem-${pkg.name}`} className="flex items-center gap-3 px-5 py-2.5 bg-severity-critical/5">
              <span className="text-[10px] font-bold text-severity-critical w-4">-</span>
              <span className="font-mono text-[12px] text-text-primary">{pkg.name}</span>
              <span className="font-mono text-[11px] text-text-secondary">{pkg.version}</span>
            </div>
          ))}
          {diff.changed.map((pkg) => (
            <div key={`chg-${pkg.name}`} className="flex items-center gap-3 px-5 py-2.5 bg-severity-medium/5">
              <span className="text-[10px] font-bold text-severity-medium w-4">~</span>
              <span className="font-mono text-[12px] text-text-primary">{pkg.name}</span>
              <span className="font-mono text-[11px] text-severity-critical line-through">{pkg.old_version}</span>
              <span className="text-text-tertiary">{'→'}</span>
              <span className="font-mono text-[11px] text-accent-mint">{pkg.new_version}</span>
            </div>
          ))}
        </div>
      ) : (
        <div className="px-5 py-8 text-center text-sm text-text-tertiary">
          No differences found -- {total} packages are identical.
        </div>
      )}
    </div>
  );
}
