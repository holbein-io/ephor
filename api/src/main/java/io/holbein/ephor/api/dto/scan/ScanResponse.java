package io.holbein.ephor.api.dto.scan;

import io.holbein.ephor.api.model.enums.ScanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Scan summary")
public record ScanResponse(
        long id,
        String namespace,
        String scanLabel,
        ScanStatus status,
        Instant startedAt,
        Instant completedAt,
        String trivyVersion
) {}
