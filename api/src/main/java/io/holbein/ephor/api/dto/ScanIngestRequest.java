package io.holbein.ephor.api.dto;

import io.holbein.ephor.api.model.enums.ScanStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ScanIngestRequest {

    @NotBlank(message = "Namespace is required")
    private String namespace;

    @NotBlank(message = "Scan label is required")
    private String scanLabel;

    private UUID scanGroupId;

    @NotNull(message = "Status is required")
    private ScanStatus status;

    private Instant startedAt;
    private Instant completedAt;
    private String trivyVersion;
    private Map<String, Object> scanConfig;

    @Valid
    @NotNull(message = "Workloads list is required")
    private List<WorkloadData> workloads = new ArrayList<>();
}
