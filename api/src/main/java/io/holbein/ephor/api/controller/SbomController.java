package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.sbom.SbomCoverageResponse;
import io.holbein.ephor.api.dto.sbom.SbomDiffResult;
import io.holbein.ephor.api.dto.sbom.SbomHistoryEntry;
import io.holbein.ephor.api.dto.sbom.SbomIngestRequest;
import io.holbein.ephor.api.dto.sbom.SbomIngestResponse;
import io.holbein.ephor.api.dto.sbom.SbomMetadata;
import io.holbein.ephor.api.dto.sbom.UnreportedCve;
import io.holbein.ephor.api.dto.sbom.UnreportedCveFilter;
import io.holbein.ephor.api.dto.sbom.UnreportedCveSummary;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.service.SbomIngestionService;
import io.holbein.ephor.api.service.SbomQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/sbom")
@RequiredArgsConstructor
@Tag(name = "SBOM", description = "SBOM ingestion, retrieval, and history")
public class SbomController {

    private static final String CYCLONEDX_MEDIA_TYPE = "application/vnd.cyclonedx+json";
    private static final String SPDX_MEDIA_TYPE = "application/spdx+json";

    private final SbomIngestionService sbomIngestionService;
    private final SbomQueryService sbomQueryService;

    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SbomIngestResponse> ingest(@Valid @RequestBody SbomIngestRequest request) {
        log.info("Received SBOM ingest request for image: {}", request.getImageReference());

        SbomIngestResponse response = sbomIngestionService.ingest(request);

        HttpStatus status = "created".equals(response.getStatus()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLatest(@RequestParam("image_reference") String imageReference) {
        Optional<SbomDocument> doc = sbomQueryService.findLatest(imageReference);

        if (doc.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SbomDocument sbom = doc.get();
        String contentType = resolveContentType(sbom.getFormat());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(sbom.getDocument());
    }

    @GetMapping("/history")
    public ResponseEntity<List<SbomHistoryEntry>> getHistory(@RequestParam("image_reference") String imageReference) {
        List<SbomHistoryEntry> history = sbomQueryService.getHistory(imageReference);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/images")
    public List<String> listImages() {
        return sbomQueryService.listImageReferences();
    }

    @GetMapping("/metadata")
    public ResponseEntity<SbomMetadata> getMetadata(@RequestParam("image_reference") String imageReference) {
        return sbomQueryService.getMetadata(imageReference)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/download")
    public ResponseEntity<Map<String, Object>> download(@RequestParam("image_reference") String imageReference) {
        Optional<SbomDocument> doc = sbomQueryService.findLatest(imageReference);

        if (doc.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SbomDocument sbom = doc.get();
        String contentType = resolveContentType(sbom.getFormat());
        String filename = String.format("sbom-%s-%s.json",
                sbom.getImageReference().replaceAll("[/:@]", "-"),
                sbom.getFormat());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(sbom.getDocument());
    }

    @GetMapping("/coverage")
    public SbomCoverageResponse getCoverage() {
        return sbomQueryService.getCoverage();
    }

    @GetMapping("/diff")
    public SbomDiffResult diff(@RequestParam("sbom_id_a") UUID sbomIdA,
                               @RequestParam("sbom_id_b") UUID sbomIdB) {
        return sbomQueryService.diff(sbomIdA, sbomIdB);
    }

    @GetMapping("/alerts/unreported")
    public Page<UnreportedCve> getUnreportedCves(
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "kev_only", required = false) Boolean kevOnly,
            @RequestParam(value = "min_epss", required = false) Double minEpss,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        UnreportedCveFilter filter = UnreportedCveFilter.builder()
                .namespace(namespace)
                .kevOnly(kevOnly)
                .minEpss(minEpss)
                .severity(severity)
                .build();
        return sbomQueryService.findUnreportedCves(filter, PageRequest.of(page, size));
    }

    @GetMapping("/alerts/unreported/summary")
    public UnreportedCveSummary getUnreportedCveSummary() {
        return sbomQueryService.summarizeUnreportedCves();
    }

    @PostMapping("/availability")
    public ResponseEntity<Map<String, Map<String, Boolean>>> checkAvailability(
            @RequestBody Map<String, List<String>> request) {
        List<String> imageReferences = request.get("image_references");
        if (imageReferences == null || imageReferences.isEmpty()) {
            return ResponseEntity.ok(Map.of("availability", Map.of()));
        }

        Set<String> existing = sbomQueryService.findExistingImageReferences(imageReferences);
        Map<String, Boolean> availability = new LinkedHashMap<>();
        for (String ref : imageReferences) {
            availability.put(ref, existing.contains(ref));
        }

        return ResponseEntity.ok(Map.of("availability", availability));
    }

    private String resolveContentType(String format) {
        return switch (format) {
            case "cyclonedx" -> CYCLONEDX_MEDIA_TYPE;
            case "spdx" -> SPDX_MEDIA_TYPE;
            default -> MediaType.APPLICATION_JSON_VALUE;
        };
    }
}
