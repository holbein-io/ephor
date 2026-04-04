package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.SbomHistoryEntry;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SbomQueryService {

    private final SbomDocumentRepository sbomDocumentRepository;

    public Optional<SbomDocument> findLatest(String imageReference) {
        return sbomDocumentRepository.findLatestByImageReference(imageReference);
    }

    public List<SbomHistoryEntry> getHistory(String imageReference) {
        return sbomDocumentRepository.findAllByImageReferenceOrderByLastSeenDesc(imageReference)
                .stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    public Set<String> findExistingImageReferences(Collection<String> imageReferences) {
        if (imageReferences == null || imageReferences.isEmpty()) {
            return Set.of();
        }
        return sbomDocumentRepository.findExistingImageReferences(imageReferences);
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
