package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PackageDiff {
    private String name;
    private String version;
    private String type;
    private String license;
}
