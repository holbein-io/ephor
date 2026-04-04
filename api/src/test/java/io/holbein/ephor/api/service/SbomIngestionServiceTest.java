package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.SbomIngestRequest;
import io.holbein.ephor.api.dto.sbom.SbomIngestResponse;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.exception.ValidationException;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SbomIngestionServiceTest extends BaseIntegrationTest {

    @Autowired
    private SbomIngestionService sbomIngestionService;

    @Autowired
    private SbomDocumentRepository sbomDocumentRepository;

    @BeforeEach
    void setUp() {
        sbomDocumentRepository.deleteAll();
    }

    @Test
    void ingest_newDocument_persistsWithCorrectFields() {
        SbomIngestRequest request = buildCycloneDxRequest("nginx:1.25", "sha256:abc123");

        SbomIngestResponse response = sbomIngestionService.ingest(request);

        assertThat(response.getStatus()).isEqualTo("created");
        assertThat(response.getId()).isNotNull();
        assertThat(response.getImageReference()).isEqualTo("nginx:1.25");
        assertThat(response.getContentHash()).isNotBlank();

        SbomDocument doc = sbomDocumentRepository.findById(response.getId()).orElseThrow();
        assertThat(doc.getFormat()).isEqualTo("cyclonedx");
        assertThat(doc.getImageDigest()).isEqualTo("sha256:abc123");
        assertThat(doc.getDocument()).containsKey("bomFormat");
    }

    @Test
    void ingest_duplicateContent_deduplicatesAndUpdatesMetadata() {
        SbomIngestRequest request = buildCycloneDxRequest("nginx:1.25", "sha256:abc123");
        SbomIngestResponse first = sbomIngestionService.ingest(request);

        UUID newScanGroupId = UUID.randomUUID();
        request.setScanGroupId(newScanGroupId);
        SbomIngestResponse second = sbomIngestionService.ingest(request);

        assertThat(second.getStatus()).isEqualTo("updated");
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(sbomDocumentRepository.count()).isEqualTo(1);

        SbomDocument doc = sbomDocumentRepository.findById(first.getId()).orElseThrow();
        assertThat(doc.getScanGroupId()).isEqualTo(newScanGroupId);
        assertThat(doc.getLastSeen()).isAfterOrEqualTo(doc.getFirstSeen());
    }

    @Test
    void ingest_sameImageDifferentContent_createsSeparateDocuments() {
        SbomIngestRequest req1 = buildCycloneDxRequest("nginx:1.25", null);
        SbomIngestRequest req2 = buildCycloneDxRequest("nginx:1.25", null);
        req2.setSbom(Map.of("bomFormat", "CycloneDX", "specVersion", "1.5", "extra", "field"));

        sbomIngestionService.ingest(req1);
        sbomIngestionService.ingest(req2);

        assertThat(sbomDocumentRepository.count()).isEqualTo(2);
    }

    @Test
    void ingest_invalidFormat_rejects() {
        SbomIngestRequest request = buildCycloneDxRequest("nginx:1.25", null);
        request.setFormat("invalid");

        assertThatThrownBy(() -> sbomIngestionService.ingest(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void ingest_formatMismatchesDocumentStructure_rejects() {
        SbomIngestRequest request = new SbomIngestRequest();
        request.setImageReference("nginx:1.25");
        request.setFormat("cyclonedx");
        request.setSbom(Map.of("spdxVersion", "SPDX-2.3"));

        assertThatThrownBy(() -> sbomIngestionService.ingest(request))
                .isInstanceOf(ValidationException.class);
    }

    private SbomIngestRequest buildCycloneDxRequest(String imageReference, String imageDigest) {
        SbomIngestRequest request = new SbomIngestRequest();
        request.setImageReference(imageReference);
        request.setImageDigest(imageDigest);
        request.setScanGroupId(UUID.randomUUID());
        request.setFormat("cyclonedx");
        request.setSbom(Map.of(
                "bomFormat", "CycloneDX",
                "specVersion", "1.5",
                "components", List.of(
                        Map.of("name", "openssl", "version", "3.0.12", "type", "library"),
                        Map.of("name", "zlib", "version", "1.2.13", "type", "library")
                )
        ));
        return request;
    }
}
