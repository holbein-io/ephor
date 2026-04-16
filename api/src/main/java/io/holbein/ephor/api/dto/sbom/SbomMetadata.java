package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SbomMetadata {
    private UUID id;
    private String imageReference;
    private String format;
    private Instant firstSeen;
    private Instant lastSeen;
    private int packageCount;
}
