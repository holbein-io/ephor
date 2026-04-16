package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreScanAlert {
    private String cveId;
    private String severity;
    private String packageName;
    private String packageVersion;
    private String title;
    private String imageReference;
    private String sbomPackageVersion;
}
