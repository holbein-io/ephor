package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

public class SbomPackageResponses {

    @Data
    @Builder
    public static class TopPackage {
        private String name;
        private String version;
        private String type;
        private long imageCount;
    }

    @Data
    @Builder
    public static class LicenseDistribution {
        private String license;
        private long packageCount;
        private long imageCount;
    }
}
