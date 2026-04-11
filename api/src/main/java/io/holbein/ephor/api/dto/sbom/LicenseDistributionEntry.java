package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LicenseDistributionEntry {
    private String license;
    private long packageCount;
    private long imageCount;
}
