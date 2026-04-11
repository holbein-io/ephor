package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SbomDiffResult {
    private String imageReference;
    private List<PackageDiff> added;
    private List<PackageDiff> removed;
    private List<PackageChangeDiff> changed;
    private int unchangedCount;
}
