package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SbomHistoryEntry {
    private UUID id;
    private String imageReference;
    private String imageDigest;
    private String contentHash;
    private String format;
    private UUID scanGroupId;
    private Instant firstSeen;
    private Instant lastSeen;
}
