package io.holbein.ephor.api.dto.triage.bulkplan;

import io.holbein.ephor.api.model.enums.DecisionStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of a decision created by bulk plan execution")
public record CreatedDecision(
        long id,
        long vulnerabilityId,
        String cveId,
        SeverityLevel severity,
        String packageName,
        DecisionStatus status
) {}
