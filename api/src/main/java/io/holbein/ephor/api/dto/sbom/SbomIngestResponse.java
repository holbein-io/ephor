package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SbomIngestResponse {
    private UUID id;
    private String imageReference;
    private String contentHash;
    private String status;
}
