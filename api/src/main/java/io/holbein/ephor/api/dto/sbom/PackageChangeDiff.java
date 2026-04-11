package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PackageChangeDiff {
    private String name;
    private String type;
    private String oldVersion;
    private String newVersion;
}
