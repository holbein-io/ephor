package io.holbein.ephor.api.mapper.scan;

import io.holbein.ephor.api.dto.scan.ScanResponse;
import io.holbein.ephor.api.entity.Scan;

public final class ScanMapper {

    private ScanMapper() {}

    public static ScanResponse toResponse(Scan entity) {
        return new ScanResponse(
                entity.getId(),
                entity.getNamespace(),
                entity.getScanLabel(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getTrivyVersion()
        );
    }
}
