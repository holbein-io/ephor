package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.SbomCoverageResponse;
import io.holbein.ephor.api.dto.sbom.SbomIngestRequest;
import io.holbein.ephor.api.dto.sbom.SbomMetadata;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SbomQueryServiceTest extends BaseIntegrationTest {

    @Autowired
    private SbomQueryService sbomQueryService;

    @Autowired
    private SbomIngestionService sbomIngestionService;

    @Autowired
    private SbomDocumentRepository sbomDocumentRepository;

    @BeforeEach
    void setUp() {
        sbomDocumentRepository.deleteAll();
    }

    @Test
    void getMetadata_extractsPackageCountFromCycloneDx() {
        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12"),
                Map.of("name", "zlib", "version", "1.2.13"),
                Map.of("name", "curl", "version", "8.5.0")
        ));

        Optional<SbomMetadata> metadata = sbomQueryService.getMetadata("nginx:1.25");

        assertThat(metadata).isPresent();
        assertThat(metadata.get().getPackageCount()).isEqualTo(3);
        assertThat(metadata.get().getFormat()).isEqualTo("cyclonedx");
        assertThat(metadata.get().getImageReference()).isEqualTo("nginx:1.25");
    }

    @Test
    void getCoverage_returnsCorrectCounts() {
        ingestCycloneDx("nginx:1.25", List.of(Map.of("name", "openssl", "version", "3.0.12")));
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "libc", "version", "2.36")));

        SbomCoverageResponse coverage = sbomQueryService.getCoverage();

        assertThat(coverage.getImagesWithSbom()).isEqualTo(2);
        assertThat(coverage.getFormatBreakdown()).containsEntry("cyclonedx", 2L);
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
