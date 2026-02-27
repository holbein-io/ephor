package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.escalation.CreateEscalationRequest;
import io.holbein.ephor.api.dto.escalation.EscalationResponse;
import io.holbein.ephor.api.dto.escalation.UpdateEscalationRequest;
import io.holbein.ephor.api.service.EscalationsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/escalations")
@RequiredArgsConstructor
@Tag(name = "Escalations", description = "Vulnerability escalation tracking")
public class EscalationsController {

    private final EscalationsService escalationsService;

    @GetMapping
    public List<EscalationResponse> getEscalations() {
        return escalationsService.getAllEscalations();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EscalationResponse createEscalation(@Valid @RequestBody CreateEscalationRequest request) {
        return escalationsService.createEscalation(request);
    }

    @PatchMapping("/{id}")
    public EscalationResponse updateEscalation(@PathVariable Long id,
                                               @Valid @RequestBody UpdateEscalationRequest request) {
        return escalationsService.updateEscalation(id, request);
    }
}
