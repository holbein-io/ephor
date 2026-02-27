package io.holbein.ephor.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanIngestResponse {
    private Long scanId;
    private int vulnerabilities;
    private int workloads;
    private int criticalVulns;
    private int autoResolved;
    private int reopened;
}
