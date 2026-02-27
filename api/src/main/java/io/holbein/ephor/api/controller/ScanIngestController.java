package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.dto.ScanIngestResponse;
import io.holbein.ephor.api.service.ScanIngestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/scans")
@RequiredArgsConstructor
@Tag(name = "Scan Ingestion", description = "Scanner data ingest endpoint")
public class ScanIngestController {

    private final ScanIngestionService scanIngestionService;

    @PostMapping("/ingest")
    public ResponseEntity<ScanIngestResponse> ingestScan(@Valid @RequestBody ScanIngestRequest request) {
        log.info("Received scan ingestion request for namespace: {}", request.getNamespace());

        ScanIngestResponse response = scanIngestionService.ingestScan(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
