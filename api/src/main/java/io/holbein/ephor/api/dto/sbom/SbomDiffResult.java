package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SbomDiffResult {
    private String imageReference;
    private List<PackageEntry> added;
    private List<PackageEntry> removed;
    private List<VersionChange> changed;
    private int unchangedCount;

    @Data
    @Builder
    public static class PackageEntry {
        private String name;
        private String version;
        private String type;
        private String license;
    }

    @Data
    @Builder
    public static class VersionChange {
        private String name;
        private String type;
        private String oldVersion;
        private String newVersion;
    }
}
