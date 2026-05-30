package io.holbein.ephor.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.holbein.ephor.api.dto.sbom.SbomIngestRequest;
import io.holbein.ephor.api.dto.sbom.SbomIngestResponse;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.exception.ValidationException;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class SbomIngestionService {

    private static final long MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> SUPPORTED_FORMATS = Set.of("cyclonedx", "spdx");

    private final SbomDocumentRepository sbomDocumentRepository;
    private final SbomIndexingService sbomIndexingService;
    private final ObjectMapper canonicalMapper;

    public SbomIngestionService(SbomDocumentRepository sbomDocumentRepository,
                                SbomIndexingService sbomIndexingService) {
        this.sbomDocumentRepository = sbomDocumentRepository;
        this.sbomIndexingService = sbomIndexingService;
        this.canonicalMapper = new ObjectMapper();
        this.canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.canonicalMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    @Transactional
    public SbomIngestResponse ingest(SbomIngestRequest request) {
        validateFormat(request.getFormat());
        validateDocumentStructure(request.getFormat(), request.getSbom());

        byte[] canonicalJson = serializeCanonical(request.getSbom());
        validateSize(canonicalJson);

        // Hash a stable projection, not the raw doc: Trivy restamps serialNumber/timestamp each run and breaks dedup.
        String contentHash = computeSha256(serializeCanonical(stableProjection(request.getFormat(), request.getSbom())));

        Optional<SbomDocument> existing = sbomDocumentRepository
                .findByImageReferenceAndContentHash(request.getImageReference(), contentHash);

        if (existing.isPresent()) {
            SbomDocument doc = existing.get();
            doc.setLastSeen(Instant.now());
            if (request.getScanGroupId() != null) {
                doc.setScanGroupId(request.getScanGroupId());
            }
            sbomDocumentRepository.save(doc);

            log.debug("SBOM dedup hit for image: {}, hash: {}", request.getImageReference(), contentHash);

            return SbomIngestResponse.builder()
                    .id(doc.getId())
                    .imageReference(doc.getImageReference())
                    .contentHash(contentHash)
                    .status("updated")
                    .build();
        }

        SbomDocument doc = SbomDocument.builder()
                .imageReference(request.getImageReference())
                .imageDigest(request.getImageDigest())
                .contentHash(contentHash)
                .format(request.getFormat())
                .scanGroupId(request.getScanGroupId())
                .document(request.getSbom())
                .build();

        doc = sbomDocumentRepository.saveAndFlush(doc);
        sbomIndexingService.indexSbom(doc);

        log.info("Stored new SBOM for image: {}, format: {}, hash: {}",
                request.getImageReference(), request.getFormat(), contentHash);

        return SbomIngestResponse.builder()
                .id(doc.getId())
                .imageReference(doc.getImageReference())
                .contentHash(contentHash)
                .status("created")
                .build();
    }

    private void validateFormat(String format) {
        if (!SUPPORTED_FORMATS.contains(format)) {
            throw ValidationException.singleField("format",
                    "Unsupported SBOM format. Supported: " + SUPPORTED_FORMATS, format);
        }
    }

    private void validateDocumentStructure(String format, Map<String, Object> sbom) {
        switch (format) {
            case "cyclonedx" -> {
                if (!sbom.containsKey("bomFormat")) {
                    throw ValidationException.singleField("sbom",
                            "CycloneDX document must contain a 'bomFormat' field");
                }
            }
            case "spdx" -> {
                if (!sbom.containsKey("spdxVersion")) {
                    throw ValidationException.singleField("sbom",
                            "SPDX document must contain a 'spdxVersion' field");
                }
            }
        }
    }

    // Copy with run-specific fields stripped, used only for hashing; the full document is still stored.
    @SuppressWarnings("unchecked")
    private Map<String, Object> stableProjection(String format, Map<String, Object> sbom) {
        Map<String, Object> copy = new LinkedHashMap<>(sbom);
        switch (format) {
            case "cyclonedx" -> {
                copy.remove("serialNumber");
                if (copy.get("metadata") instanceof Map<?, ?> metadata) {
                    Map<String, Object> metadataCopy = new LinkedHashMap<>((Map<String, Object>) metadata);
                    metadataCopy.remove("timestamp");
                    copy.put("metadata", metadataCopy);
                }
            }
            case "spdx" -> {
                copy.remove("documentNamespace");
                if (copy.get("creationInfo") instanceof Map<?, ?> creationInfo) {
                    Map<String, Object> creationInfoCopy = new LinkedHashMap<>((Map<String, Object>) creationInfo);
                    creationInfoCopy.remove("created");
                    copy.put("creationInfo", creationInfoCopy);
                }
            }
        }
        return copy;
    }

    private byte[] serializeCanonical(Map<String, Object> sbom) {
        try {
            return canonicalMapper.writeValueAsBytes(sbom);
        } catch (JsonProcessingException e) {
            throw ValidationException.singleField("sbom", "Failed to serialize SBOM document");
        }
    }

    private void validateSize(byte[] data) {
        if (data.length > MAX_DOCUMENT_SIZE_BYTES) {
            throw ValidationException.singleField("sbom",
                    String.format("SBOM document exceeds maximum size of %d bytes (actual: %d)",
                            MAX_DOCUMENT_SIZE_BYTES, data.length));
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
