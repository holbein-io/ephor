package io.holbein.ephor.api.dto;

import io.holbein.ephor.api.dto.vulnerability.VulnerabilityData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class ContainerData {

    @NotBlank(message = "Container name is required")
    private String name;

    private String imageName;
    private String imageTag;
    private Instant imageCreated;
    private Instant baseImageCreated;

    @Valid
    private List<VulnerabilityData> vulnerabilities = new ArrayList<>();
}
