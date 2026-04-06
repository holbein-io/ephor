package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.PackageSearchResult;
import io.holbein.ephor.api.dto.sbom.TopPackageEntry;
import io.holbein.ephor.api.entity.SbomPackage;
import io.holbein.ephor.api.repositories.SbomPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SbomPackageQueryService {

    private final SbomPackageRepository sbomPackageRepository;

    public Page<PackageSearchResult> searchPackages(String name, String type, Pageable pageable) {
        Page<SbomPackage> packages = type != null
                ? sbomPackageRepository.searchByNameAndType(name, type, pageable)
                : sbomPackageRepository.searchByName(name, pageable);

        return packages.map(this::toSearchResult);
    }

    public List<String> findImagesByPackage(String name, String version) {
        return version != null
                ? sbomPackageRepository.findImagesByPackageNameAndVersion(name, version)
                : sbomPackageRepository.findImagesByPackageName(name);
    }

    public Page<TopPackageEntry> getTopPackages(Pageable pageable) {
        return sbomPackageRepository.findTopPackages(pageable)
                .map(row -> TopPackageEntry.builder()
                        .name((String) row[0])
                        .version((String) row[1])
                        .type((String) row[2])
                        .imageCount((Long) row[3])
                        .build());
    }

    private PackageSearchResult toSearchResult(SbomPackage pkg) {
        return PackageSearchResult.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .type(pkg.getType())
                .purl(pkg.getPurl())
                .license(pkg.getLicense())
                .imageReference(pkg.getImageReference())
                .build();
    }
}
