package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.sbom.SbomHistoryEntry;
import io.holbein.ephor.api.dto.sbom.SbomIngestRequest;
import io.holbein.ephor.api.dto.sbom.SbomIngestResponse;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.service.SbomIngestionService;
import io.holbein.ephor.api.service.SbomQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
