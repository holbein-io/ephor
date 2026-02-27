package io.holbein.ephor.api.dto.triage.preparation;

import io.holbein.ephor.api.dto.triage.shared.WorkloadSummary;
import io.holbein.ephor.api.model.enums.PreliminaryDecision;
import io.holbein.ephor.api.model.enums.PrepStatus;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Triage preparation details with denormalized vulnerability data")
public record PreparationResponse(
        long id,
        long sessionId,
        long vulnerabilityId,
        PrepStatus prepStatus,
        String prepNotes,
        PriorityFlag priorityFlag,
        PreliminaryDecision preliminaryDecision,
        String prepBy,
        Instant prepAt,

        // Denormalized from Vulnerability
        String cveId,
        String packageName,
        String packageVersion,
        SeverityLevel severity,
        String title,
        String description,
        String primaryUrl,
        String fixedVersion,

        @Schema(description = "Affected workloads, included when include_workloads=true")
        List<WorkloadSummary> affectedWorkloads
) {}
