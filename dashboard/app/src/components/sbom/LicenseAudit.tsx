import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Scale, ChevronRight } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { sbomService } from '../../services/api';

const COPYLEFT_LICENSES = ['AGPL-3.0', 'GPL-2.0', 'GPL-3.0', 'LGPL-2.1', 'LGPL-3.0', 'SSPL-1.0'];

function isCopyleft(license: string): boolean {
  return COPYLEFT_LICENSES.some(cl => license.toUpperCase().includes(cl.toUpperCase()));
}

export function LicenseAudit() {
  const [selectedLicense, setSelectedLicense] = useState<string | null>(null);

  const { data: distribution, isLoading } = useQuery({
    queryKey: ['license-distribution'],
    queryFn: () => sbomService.getLicenseDistribution(),
  });

  const { data: licensePackages } = useQuery({
    queryKey: ['license-packages', selectedLicense],
    queryFn: () => sbomService.searchByLicense(selectedLicense!, 0, 50),
    enabled: !!selectedLicense,
  });

  if (isLoading) {
    return (
      <div className="px-6 py-12 text-center">
        <div className="inline-block h-5 w-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!distribution?.length) {
    return (
      <div className="px-6 py-12 text-center">
        <Scale className="h-8 w-8 text-text-tertiary mx-auto mb-3" />
        <p className="text-sm text-text-tertiary">No license data available. Ingest SBOMs with license information to populate this view.</p>
      </div>
    );
  }

  const chartData = distribution.slice(0, 15).map(d => ({
    license: d.license.length > 20 ? d.license.substring(0, 20) + '...' : d.license,
    fullLicense: d.license,
    packages: d.package_count,
    images: d.image_count,
    copyleft: isCopyleft(d.license),
  }));

  return (
    <div className="space-y-4">
      <div className="bg-bg-secondary border border-border rounded-2xl p-5">
        <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
          License Distribution
        </span>
        <div className="mt-4 h-[250px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} layout="vertical" margin={{ left: 120, right: 20 }}>
              <XAxis type="number" tick={{ fontSize: 11, fill: 'var(--color-text-tertiary)' }} />
              <YAxis
                type="category"
                dataKey="license"
                tick={{ fontSize: 11, fill: 'var(--color-text-secondary)', fontFamily: 'monospace' }}
                width={120}
              />
              <Tooltip
                contentStyle={{
                  background: 'var(--color-bg-secondary)',
                  border: '1px solid var(--color-border)',
                  borderRadius: '8px',
                  fontSize: '12px',
                }}
                formatter={(value: number, name: string) => [value, name === 'packages' ? 'Packages' : 'Images']}
                labelFormatter={(label: string, payload: any[]) => payload?.[0]?.payload?.fullLicense || label}
              />
              <Bar dataKey="packages" name="packages" radius={[0, 4, 4, 0]}>
                {chartData.map((entry, i) => (
                  <Cell
                    key={i}
                    fill={entry.copyleft ? 'var(--color-severity-high)' : 'var(--color-accent)'}
                    cursor="pointer"
                    onClick={() => setSelectedLicense(entry.fullLicense)}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="flex items-center gap-4 mt-2 text-[11px] text-text-tertiary">
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm" style={{ background: 'var(--color-accent)' }} />
            Permissive
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm" style={{ background: 'var(--color-severity-high)' }} />
            Copyleft
          </span>
        </div>
      </div>

      <div className="bg-bg-secondary border border-border rounded-2xl overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border">
              <th className="px-5 py-3 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">License</th>
              <th className="px-5 py-3 text-right text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Packages</th>
              <th className="px-5 py-3 text-right text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Images</th>
              <th className="px-5 py-3 w-8"></th>
            </tr>
          </thead>
          <tbody>
            {distribution.map((entry) => (
              <tr
                key={entry.license}
                onClick={() => setSelectedLicense(selectedLicense === entry.license ? null : entry.license)}
                className={`border-b border-border/50 last:border-b-0 cursor-pointer transition-colors ${
                  selectedLicense === entry.license ? 'bg-accent/5' : 'hover:bg-bg-hover'
                }`}
              >
                <td className="px-5 py-3.5">
                  <span className="font-mono text-[13px] font-medium text-text-primary">
                    {entry.license}
                  </span>
                  {isCopyleft(entry.license) && (
                    <span className="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold bg-severity-high/10 text-severity-high border border-severity-high/20">
                      COPYLEFT
                    </span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-right font-mono text-[13px] text-text-secondary">
                  {entry.package_count}
                </td>
                <td className="px-5 py-3.5 text-right font-mono text-[13px] text-text-secondary">
                  {entry.image_count}
                </td>
                <td className="px-5 py-3.5">
                  <ChevronRight className={`h-3.5 w-3.5 text-text-tertiary transition-transform ${
                    selectedLicense === entry.license ? 'rotate-90' : ''
                  }`} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedLicense && licensePackages?.content && (
        <div className="bg-bg-secondary border border-accent/20 rounded-2xl overflow-hidden animate-fade-up">
          <div className="px-5 py-3 border-b border-border">
            <span className="font-mono text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">
              Packages with license: {selectedLicense}
            </span>
          </div>
          <table className="w-full">
            <thead>
              <tr className="border-b border-border">
                <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Package</th>
                <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Version</th>
                <th className="px-5 py-2.5 text-left text-[11px] font-bold tracking-[0.1em] uppercase text-text-tertiary">Image</th>
              </tr>
            </thead>
            <tbody>
              {licensePackages.content.map((pkg, i) => (
                <tr key={`${pkg.name}-${i}`} className="border-b border-border/50 last:border-b-0">
                  <td className="px-5 py-2.5 font-mono text-[12px] text-text-primary">{pkg.name}</td>
                  <td className="px-5 py-2.5 font-mono text-[12px] text-text-secondary">{pkg.version}</td>
                  <td className="px-5 py-2.5 font-mono text-[11px] text-text-tertiary">{pkg.image_reference}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
