import { useQuery } from '@tanstack/react-query';
import { Download, FileText } from 'lucide-react';
import { sbomService } from '../../services/api';

interface SbomBadgeProps {
  imageReference: string;
}

export function SbomBadge({ imageReference }: SbomBadgeProps) {
  const { data: metadata, isLoading } = useQuery({
    queryKey: ['sbom-metadata', imageReference],
    queryFn: () => sbomService.getMetadata(imageReference),
    retry: false,
  });

  if (isLoading) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono bg-bg-tertiary text-text-tertiary animate-pulse">
        SBOM
      </span>
    );
  }

  if (!metadata) {
    return null;
  }

  return (
    <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-accent-mint/10 text-accent-mint border border-accent-mint/20">
      <FileText className="h-3 w-3" />
      <span>{metadata.format.toUpperCase()}</span>
      <span className="text-accent-mint/60">{metadata.package_count} pkg</span>
      <a
        href={sbomService.getDownloadUrl(imageReference)}
        title="Download SBOM"
        className="ml-0.5 hover:text-accent-mint transition-colors"
        onClick={(e) => e.stopPropagation()}
      >
        <Download className="h-3 w-3" />
      </a>
    </span>
  );
}
