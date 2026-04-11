import { apiClient } from './client';
import { SbomMetadata, SbomHistoryEntry, SbomCoverage, PackageSearchResult, TopPackageEntry, PageResponse, LicenseDistributionEntry, SbomDiffResult, PreScanAlert } from '../../types';

export const sbomService = {
  async getMetadata(imageReference: string): Promise<SbomMetadata> {
    return apiClient.get('/sbom/metadata', { image_reference: imageReference });
  },

  async getHistory(imageReference: string): Promise<SbomHistoryEntry[]> {
    return apiClient.get('/sbom/history', { image_reference: imageReference });
  },

  async getCoverage(): Promise<SbomCoverage> {
    return apiClient.get('/sbom/coverage');
  },

  async checkAvailability(imageReferences: string[]): Promise<{ availability: Record<string, boolean> }> {
    return apiClient.post('/sbom/availability', { image_references: imageReferences });
  },

  getDownloadUrl(imageReference: string): string {
    return `/api/v1/sbom/download?image_reference=${encodeURIComponent(imageReference)}`;
  },

  async searchPackages(name: string, type?: string, page = 0, size = 25): Promise<PageResponse<PackageSearchResult>> {
    return apiClient.get('/sbom/packages/search', { name, type, page, size });
  },

  async getImagesByPackage(name: string, version?: string): Promise<string[]> {
    return apiClient.get('/sbom/packages/images', { name, version });
  },

  async getTopPackages(size = 20): Promise<PageResponse<TopPackageEntry>> {
    return apiClient.get('/sbom/packages/top', { size });
  },

  async getLicenseDistribution(): Promise<LicenseDistributionEntry[]> {
    return apiClient.get('/sbom/packages/licenses');
  },

  async searchByLicense(license: string, page = 0, size = 25): Promise<PageResponse<PackageSearchResult>> {
    return apiClient.get('/sbom/packages/licenses/search', { license, page, size });
  },

  async getDiff(sbomIdA: string, sbomIdB: string): Promise<SbomDiffResult> {
    return apiClient.get('/sbom/diff', { sbom_id_a: sbomIdA, sbom_id_b: sbomIdB });
  },

  async getPreScanAlerts(limit = 50): Promise<PreScanAlert[]> {
    return apiClient.get('/sbom/alerts/pre-scan', { limit });
  },

  async getPreScanAlertCount(): Promise<{ count: number }> {
    return apiClient.get('/sbom/alerts/pre-scan/count');
  },
};
