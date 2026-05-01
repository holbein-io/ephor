package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SbomCoverageResponse {
    private long totalImages;
    private long imagesWithSbom;
    private Map<String, Long> formatBreakdown;
}
