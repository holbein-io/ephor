package io.holbein.ephor.api.dto.sbom;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class UnreportedCve {
    private String cveId;
    private String imageReference;
    private long vulnerabilityId;
    private String severity;
    private boolean kevListed;
    private Double epssScore;
    private Instant publishedDate;
    private String title;
    private List<String> fixedVersions;
    private List<String> packages;
    private List<String> namespaces;
    private List<String> workloads;
}
