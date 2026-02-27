package io.holbein.ephor.api.dto;

import io.holbein.ephor.api.entity.VulnerabilityInstance.InstanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private InstanceStatus status;
}
