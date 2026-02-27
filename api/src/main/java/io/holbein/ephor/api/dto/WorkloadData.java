package io.holbein.ephor.api.dto;

import io.holbein.ephor.api.entity.Workload.WorkloadKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkloadData {

    @NotBlank(message = "Namespace is required")
    private String namespace;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Kind is required")
    private WorkloadKind kind;

    @Valid
    @NotEmpty(message = "Containers list is required and must not be empty")
    private List<ContainerData> containers = new ArrayList<>();
}
