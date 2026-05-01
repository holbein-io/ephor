package io.holbein.ephor.api.service;

import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.entity.SbomPackage;
import io.holbein.ephor.api.repositories.SbomPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SbomIndexingService {

    private final SbomPackageRepository sbomPackageRepository;

    @Transactional
    public int indexSbom(SbomDocument document) {
        sbomPackageRepository.deleteBySbomDocumentId(document.getId());

        List<SbomPackage> packages = switch (document.getFormat()) {
            case "cyclonedx" -> parseCycloneDxComponents(document);
            case "spdx" -> parseSpdxPackages(document);
            default -> List.of();
        };

        if (packages.isEmpty()) {
            return 0;
        }

        sbomPackageRepository.saveAll(packages);

        log.info("Indexed {} packages for image: {}", packages.size(), document.getImageReference());
        return packages.size();
    }

    private List<SbomPackage> parseCycloneDxComponents(SbomDocument document) {
        Object componentsObj = document.getDocument().get("components");
        if (!(componentsObj instanceof List<?> components)) {
            return List.of();
        }

        List<SbomPackage> packages = new ArrayList<>();
        for (Object item : components) {
            if (!(item instanceof Map<?, ?> comp)) continue;

            String name = getString(comp, "name");
            String version = getString(comp, "version");
            if (name == null || version == null) continue;

            SbomPackage pkg = SbomPackage.builder()
                    .sbomDocument(document)
                    .imageReference(document.getImageReference())
                    .name(name)
                    .version(version)
                    .purl(getString(comp, "purl"))
                    .type(extractTypeFromPurl(getString(comp, "purl")))
                    .license(extractCycloneDxLicense(comp))
                    .supplier(extractCycloneDxSupplier(comp))
                    .build();
            packages.add(pkg);
        }
        return packages;
    }

    private List<SbomPackage> parseSpdxPackages(SbomDocument document) {
        Object packagesObj = document.getDocument().get("packages");
        if (!(packagesObj instanceof List<?> spdxPackages)) {
            return List.of();
        }

        List<SbomPackage> packages = new ArrayList<>();
        for (Object item : spdxPackages) {
            if (!(item instanceof Map<?, ?> pkg)) continue;

            String name = getString(pkg, "name");
            String version = getString(pkg, "versionInfo");
            if (name == null || version == null) continue;

            String purl = extractSpdxPurl(pkg);

            SbomPackage sbomPkg = SbomPackage.builder()
                    .sbomDocument(document)
                    .imageReference(document.getImageReference())
                    .name(name)
                    .version(version)
                    .purl(purl)
                    .type(extractTypeFromPurl(purl))
                    .license(getString(pkg, "licenseDeclared"))
                    .supplier(getString(pkg, "supplier"))
                    .build();
            packages.add(sbomPkg);
        }
        return packages;
    }

    private String extractTypeFromPurl(String purl) {
        if (purl == null || !purl.startsWith("pkg:")) return null;
        int slashIndex = purl.indexOf('/', 4);
        return slashIndex > 4 ? purl.substring(4, slashIndex) : null;
    }

    private String extractCycloneDxLicense(Map<?, ?> component) {
        Object licensesObj = component.get("licenses");
        if (!(licensesObj instanceof List<?> licenses) || licenses.isEmpty()) return null;

        Object first = licenses.get(0);
        if (!(first instanceof Map<?, ?> licenseEntry)) return null;

        String expression = getString(licenseEntry, "expression");
        if (expression != null) return expression;

        Object licenseObj = licenseEntry.get("license");
        if (licenseObj instanceof Map<?, ?> license) {
            return getString(license, "id");
        }
        return null;
    }

    private String extractCycloneDxSupplier(Map<?, ?> component) {
        Object supplierObj = component.get("supplier");
        if (supplierObj instanceof Map<?, ?> supplier) {
            return getString(supplier, "name");
        }
        return null;
    }

    private String extractSpdxPurl(Map<?, ?> pkg) {
        Object externalRefs = pkg.get("externalRefs");
        if (!(externalRefs instanceof List<?> refs)) return null;

        for (Object ref : refs) {
            if (!(ref instanceof Map<?, ?> refMap)) continue;
            if ("PACKAGE-MANAGER".equals(getString(refMap, "referenceCategory")) ||
                "PACKAGE_MANAGER".equals(getString(refMap, "referenceCategory"))) {
                String locator = getString(refMap, "referenceLocator");
                if (locator != null && locator.startsWith("pkg:")) return locator;
            }
        }
        return null;
    }

    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof String s ? s : null;
    }
}
