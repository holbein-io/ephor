package io.holbein.ephor.api.dto.sbom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class SbomIngestRequest {

    @NotBlank(message = "Image reference is required")
    private String imageReference;

    private String imageDigest;

    private UUID scanGroupId;

    @NotBlank(message = "Format is required")
    private String format;

    @NotNull(message = "SBOM document is required")
    private Map<String, Object> sbom;
}
