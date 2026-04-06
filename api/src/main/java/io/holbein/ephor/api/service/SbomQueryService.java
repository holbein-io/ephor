package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.SbomCoverageResponse;
import io.holbein.ephor.api.dto.sbom.SbomHistoryEntry;
import io.holbein.ephor.api.dto.sbom.SbomMetadata;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.repositories.ContainerRepository;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SbomQueryService {

    private final SbomDocumentRepository sbomDocumentRepository;
    private final ContainerRepository containerRepository;

    public Optional<SbomDocument> findLatest(String imageReference) {
        return sbomDocumentRepository.findLatestByImageReference(imageReference);
    }

    public List<SbomHistoryEntry> getHistory(String imageReference) {
        return sbomDocumentRepository.findAllByImageReferenceOrderByLastSeenDesc(imageReference)
                .stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    public Optional<SbomMetadata> getMetadata(String imageReference) {
        return sbomDocumentRepository.findLatestByImageReference(imageReference)
                .map(this::toMetadata);
    }

    public SbomCoverageResponse getCoverage() {
        long totalImages = containerRepository.countDistinctImages();
        long imagesWithSbom = sbomDocumentRepository.countDistinctImageReferences();

        Map<String, Long> formatBreakdown = new LinkedHashMap<>();
        for (Object[] row : sbomDocumentRepository.countByFormat()) {
            formatBreakdown.put((String) row[0], (Long) row[1]);
        }

        return SbomCoverageResponse.builder()
                .totalImages(totalImages)
                .imagesWithSbom(imagesWithSbom)
                .formatBreakdown(formatBreakdown)
                .build();
    }

    public Set<String> findExistingImageReferences(Collection<String> imageReferences) {
        if (imageReferences == null || imageReferences.isEmpty()) {
            return Set.of();
        }
        return sbomDocumentRepository.findExistingImageReferences(imageReferences);
    }

    private SbomMetadata toMetadata(SbomDocument doc) {
        return SbomMetadata.builder()
                .id(doc.getId())
                .imageReference(doc.getImageReference())
                .format(doc.getFormat())
                .firstSeen(doc.getFirstSeen())
                .lastSeen(doc.getLastSeen())
                .packageCount(extractPackageCount(doc))
                .build();
    }

    private int extractPackageCount(SbomDocument doc) {
        Map<String, Object> document = doc.getDocument();
        if (document == null) {
            return 0;
        }
        return switch (doc.getFormat()) {
            case "cyclonedx" -> {
                Object components = document.get("components");
                yield components instanceof List<?> list ? list.size() : 0;
            }
            case "spdx" -> {
                Object packages = document.get("packages");
                yield packages instanceof List<?> list ? list.size() : 0;
            }
            default -> 0;
        };
    }

    private SbomHistoryEntry toHistoryEntry(SbomDocument doc) {
        return SbomHistoryEntry.builder()
                .id(doc.getId())
                .imageReference(doc.getImageReference())
                .imageDigest(doc.getImageDigest())
                .contentHash(doc.getContentHash())
                .format(doc.getFormat())
                .scanGroupId(doc.getScanGroupId())
                .firstSeen(doc.getFirstSeen())
                .lastSeen(doc.getLastSeen())
                .build();
    }
}
