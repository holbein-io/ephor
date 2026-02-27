package io.holbein.ephor.api.dto.triage.bulkplan;

import io.holbein.ephor.api.model.enums.PrepStatus;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Typed filter criteria for bulk plan matching against session preparations")
public record BulkPlanFilters(

        @Schema(description = "Filter by vulnerability severity levels")
        List<SeverityLevel> severity,

        @Schema(description = "Filter by workload namespace")
        List<String> namespace,

        @Schema(description = "Filter by exact package name")
        List<String> packageName,

        @Schema(description = "Filter by package name regex pattern", example = "^linux-kernel")
        String packageNamePattern,

        @Schema(description = "Filter by CVE ID regex pattern", example = "CVE-2025-.*")
        String cvePattern,

        @Schema(description = "Filter by preparation priority flag")
        List<PriorityFlag> priorityFlag,

        @Schema(description = "Filter by preparation status")
        List<PrepStatus> prepStatus
) {}
