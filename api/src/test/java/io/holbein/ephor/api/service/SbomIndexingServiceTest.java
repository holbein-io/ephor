package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.SbomIngestRequest;
import io.holbein.ephor.api.dto.sbom.TopPackageEntry;
import io.holbein.ephor.api.entity.SbomPackage;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import io.holbein.ephor.api.repositories.SbomPackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SbomIndexingServiceTest extends BaseIntegrationTest {

    @Autowired
    private SbomIngestionService sbomIngestionService;

    @Autowired
    private SbomPackageQueryService sbomPackageQueryService;

    @Autowired
    private SbomDocumentRepository sbomDocumentRepository;

    @Autowired
    private SbomPackageRepository sbomPackageRepository;

    @BeforeEach
    void setUp() {
        sbomPackageRepository.deleteAll();
        sbomDocumentRepository.deleteAll();
    }

    @Test
    void ingest_indexesCycloneDxComponents() {
        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12", "purl", "pkg:debian/openssl@3.0.12"),
                Map.of("name", "zlib", "version", "1.2.13", "purl", "pkg:debian/zlib@1.2.13")
        ));

        List<SbomPackage> packages = sbomPackageRepository.findAll();
        assertThat(packages).hasSize(2);
        assertThat(packages).extracting(SbomPackage::getName).containsExactlyInAnyOrder("openssl", "zlib");
        assertThat(packages).extracting(SbomPackage::getType).containsOnly("debian");
        assertThat(packages).extracting(SbomPackage::getImageReference).containsOnly("nginx:1.25");
    }

    @Test
    void ingest_reindexesOnNewSbomForSameImage() {
        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.11")
        ));
        assertThat(sbomPackageRepository.count()).isEqualTo(1);

        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12"),
                Map.of("name", "curl", "version", "8.5.0")
        ));
        assertThat(sbomPackageRepository.count()).isEqualTo(3);
    }

    @Test
    void searchPackages_findsByPartialName() {
        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12"),
                Map.of("name", "curl", "version", "8.5.0")
        ));

        var results = sbomPackageQueryService.searchPackages("ssl", null, PageRequest.of(0, 10));
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("openssl");
    }

    @Test
    void topPackages_orderedByImageCount() {
        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12"),
                Map.of("name", "zlib", "version", "1.2.13")
        ));
        ingestCycloneDx("redis:7.2", List.of(
                Map.of("name", "openssl", "version", "3.0.12")
        ));

        Page<TopPackageEntry> top = sbomPackageQueryService.getTopPackages(PageRequest.of(0, 10));
        assertThat(top.getContent().get(0).getName()).isEqualTo("openssl");
        assertThat(top.getContent().get(0).getImageCount()).isEqualTo(2);
    }

    private void ingestCycloneDx(String imageReference, List<Map<String, String>> components) {
        SbomIngestRequest request = new SbomIngestRequest();
        request.setImageReference(imageReference);
        request.setScanGroupId(UUID.randomUUID());
        request.setFormat("cyclonedx");
        request.setSbom(Map.of(
                "bomFormat", "CycloneDX",
                "specVersion", "1.5",
                "components", components
        ));
        sbomIngestionService.ingest(request);
    }
}
