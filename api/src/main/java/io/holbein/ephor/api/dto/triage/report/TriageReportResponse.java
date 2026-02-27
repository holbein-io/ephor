package io.holbein.ephor.api.dto.triage.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Triage report containing vulnerabilities available for session preparation")
public record TriageReportResponse(

        @Schema(description = "Lookback period in days", example = "7")
        int days,

        @Schema(description = "Namespace filter applied, null if not filtered")
        String namespace,

        @Schema(description = "Total number of matching vulnerabilities")
        int total,

        @Schema(description = "List of vulnerabilities available for preparation")
        List<VulnerabilityForTriage> vulnerabilities
) {}
