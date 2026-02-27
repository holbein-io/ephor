package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.triage.bulkplan.BulkPlanResponse;
import io.holbein.ephor.api.dto.triage.bulkplan.CreateBulkPlanRequest;
import io.holbein.ephor.api.dto.triage.bulkplan.ExecuteBulkPlanRequest;
import io.holbein.ephor.api.dto.triage.bulkplan.ExecuteBulkPlanResponse;
import io.holbein.ephor.api.dto.triage.decision.CreateDecisionRequest;
import io.holbein.ephor.api.dto.triage.decision.DecisionResponse;
import io.holbein.ephor.api.dto.triage.preparation.AddPreparationRequest;
import io.holbein.ephor.api.dto.triage.preparation.PreparationResponse;
import io.holbein.ephor.api.dto.triage.preparation.UpdatePreparationRequest;
import io.holbein.ephor.api.dto.triage.report.TriageReportResponse;
import io.holbein.ephor.api.dto.triage.session.ChangeSessionStatusRequest;
import io.holbein.ephor.api.dto.triage.session.CreateSessionRequest;
import io.holbein.ephor.api.dto.triage.session.SessionResponse;
import io.holbein.ephor.api.dto.triage.session.UpdateSessionRequest;
import io.holbein.ephor.api.dto.triage.shared.DeleteResponse;
import io.holbein.ephor.api.model.enums.BulkPlanStatus;
import io.holbein.ephor.api.model.enums.SessionStatus;
import io.holbein.ephor.api.service.TriageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/triage")
@RequiredArgsConstructor
@Tag(name = "Triage", description = "Sessions, preparations, decisions, bulk plans")
public class TriageController {

    private final TriageService triageService;

    @GetMapping("/sessions")
    public List<SessionResponse> getTriageSessions(@RequestParam(required = false) SessionStatus status) {
        return triageService.getTriageSessions(status);
    }

    @PostMapping("/sessions")
    public SessionResponse createTriageSession(@Valid @RequestBody CreateSessionRequest request) {
        return triageService.createTriageSession(request);
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getTriageSession(@PathVariable long id) {
        return triageService.getTriageSession(id);
    }

    @PatchMapping("/sessions/{id}")
    public SessionResponse updateTriageSession(@PathVariable long id,
                                               @Valid @RequestBody UpdateSessionRequest request) {
        return triageService.updateTriageSession(id, request);
    }

    @DeleteMapping("/sessions/{id}")
    public DeleteResponse deleteTriageSession(@PathVariable long id) {
        return triageService.deleteTriageSession(id);
    }

    @PatchMapping("/sessions/{id}/status")
    public SessionResponse changeSessionStatus(@PathVariable long id,
                                               @Valid @RequestBody ChangeSessionStatusRequest request) {
        return triageService.changeSessionStatus(id, request);
    }

    @GetMapping("/sessions/{id}/preparations")
    public List<PreparationResponse> getSessionPreparations(@PathVariable long id,
                                                            @RequestParam(required = false, name = "include_workloads") boolean includeWorkloads) {
        return triageService.getSessionPreparations(id, includeWorkloads);
    }

    @PostMapping("/preparations")
    public PreparationResponse createPreparation(@Valid @RequestBody AddPreparationRequest request) {
        return triageService.createPreparation(request);
    }

    @PatchMapping("/preparations/{preparationId}")
    public PreparationResponse updatePreparation(@PathVariable long preparationId,
                                                 @Valid @RequestBody UpdatePreparationRequest request) {
        return triageService.updatePreparation(preparationId, request);
    }

    @DeleteMapping("/preparations/{preparationId}")
    public DeleteResponse deletePreparation(@PathVariable long preparationId) {
        return triageService.deletePreparation(preparationId);
    }

    @GetMapping("/report")
    public TriageReportResponse getTriageReport(@RequestParam int days,
                                                @RequestParam(required = false) String namespace,
                                                @RequestParam(required = false) List<String> severity,
                                                @RequestParam(required = false, name = "exclude_decided") boolean excludeDecided) {
        return triageService.getTriageReport(days, namespace, severity, excludeDecided);
    }

    @PostMapping("/decisions")
    public DecisionResponse createDecision(@Valid @RequestBody CreateDecisionRequest request) {
        return triageService.createDecision(request);
    }

    @GetMapping("/sessions/{sessionId}/decisions")
    public List<DecisionResponse> getSessionDecisions(@PathVariable long sessionId) {
        return triageService.getSessionDecisions(sessionId);
    }

    @PostMapping("/bulk-plans")
    public BulkPlanResponse createBulkPlan(@Valid @RequestBody CreateBulkPlanRequest request) {
        return triageService.createBulkPlan(request);
    }

    @GetMapping("/sessions/{sessionId}/bulk-plans")
    public List<BulkPlanResponse> getSessionBulkPlans(@PathVariable long sessionId,
                                                      @RequestParam(required = false) BulkPlanStatus status) {
        return triageService.getSessionBulkPlans(sessionId, status);
    }

    @PostMapping("/bulk-plans/{planId}/execute")
    public ExecuteBulkPlanResponse executeBulkPlan(@PathVariable long planId,
                                                   @Valid @RequestBody ExecuteBulkPlanRequest request) {
        return triageService.executeBulkPlan(planId, request);
    }

    @PatchMapping("/bulk-plans/{planId}")
    public BulkPlanResponse cancelBulkPlan(@PathVariable long planId) {
        return triageService.cancelBulkPlan(planId);
    }

    @GetMapping("/bulk-plans/{planId}/preview")
    public BulkPlanResponse previewBulkPlan(@PathVariable long planId) {
        return triageService.previewBulkPlan(planId);
    }

    @DeleteMapping("/bulk-plans/{planId}")
    public DeleteResponse deleteBulkPlan(@PathVariable long planId) {
        return triageService.deleteBulkPlan(planId);
    }
}
