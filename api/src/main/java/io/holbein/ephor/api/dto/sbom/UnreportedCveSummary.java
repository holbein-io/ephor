package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnreportedCveSummary {
    private long total;
    private long kevListed;
    private long epssHigh;
    private long images;
}
