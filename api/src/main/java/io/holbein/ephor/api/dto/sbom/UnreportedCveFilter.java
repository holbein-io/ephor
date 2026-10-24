package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnreportedCveFilter {
    private String namespace;
    private Boolean kevOnly;
    private Double minEpss;
    private String severity;
}
