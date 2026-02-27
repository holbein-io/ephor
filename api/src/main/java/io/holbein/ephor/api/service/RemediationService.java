package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.remediation.*;
import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.entity.RemediationComment;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.mapper.remediation.RemediationCommentMapper;
import io.holbein.ephor.api.mapper.remediation.RemediationMapper;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import io.holbein.ephor.api.repositories.RemediationCommentRepository;
import io.holbein.ephor.api.repositories.RemediationRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemediationService {

    private final RemediationRepository remediationRepository;
    private final RemediationCommentRepository remediationCommentRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    private static final List<RemediationStatus> ACTIVE_STATUSES =
            List.of(RemediationStatus.planned, RemediationStatus.in_progress);

    private static final Set<String> VALID_TRANSITIONS_FROM_PLANNED =
            Set.of("in_progress", "abandoned");
    private static final Set<String> VALID_TRANSITIONS_FROM_IN_PROGRESS =
            Set.of("completed", "abandoned");

    public List<RemediationResponse> getRemediationsByVulnerability(Long vulnerabilityId) {
        return remediationRepository.findAllByVulnerabilityId(vulnerabilityId).stream()
                .map(RemediationMapper::toResponse)
                .toList();
    }

    public RemediationDetailResponse getRemediation(Long id) {
        return remediationRepository.findById(id)
                .map(RemediationMapper::toDetailResponse)
                .orElse(null);
    }

    @Transactional
    public RemediationResponse createRemediation(CreateRemediationRequest request) {
        Vulnerability vulnerability = vulnerabilityRepository.getVulnerabilityById(request.vulnerabilityId());
        Remediation remediation = RemediationMapper.toEntity(request, vulnerability);
        remediationRepository.save(remediation);
        return RemediationMapper.toResponse(remediation);
    }

    @Transactional
    public RemediationResponse updateRemediation(Long id, UpdateRemediationRequest request) {
        Remediation remediation = remediationRepository.getReferenceById(id);

        if (request.assignedTo() != null) {
            remediation.setAssignedTo(request.assignedTo());
        }
        if (request.targetDate() != null) {
            remediation.setTargetDate(request.targetDate());
        }
        if (request.priority() != null) {
            remediation.setPriority(request.priority());
        }
        if (request.notes() != null) {
            remediation.setNotes(request.notes());
        }

        remediationRepository.save(remediation);
        return RemediationMapper.toResponse(remediation);
    }

    @Transactional
    public RemediationResponse changeRemediationStatus(Long id, ChangeRemediationStatusRequest request) {
        Remediation remediation = remediationRepository.getReferenceById(id);
        RemediationStatus currentStatus = remediation.getStatus();
        RemediationStatus targetStatus = request.status();

        validateTransition(currentStatus, targetStatus);

        Instant now = Instant.now();

        switch (targetStatus) {
            case in_progress -> {}
            case completed -> {
                if (request.completionMethod() == null) {
                    throw new IllegalArgumentException("completionMethod is required when completing a remediation");
                }
                if (request.completedBy() == null) {
                    throw new IllegalArgumentException("completedBy is required when completing a remediation");
                }
                remediation.setCompletedAt(now);
                remediation.setCompletionMethod(request.completionMethod());
                remediation.setCompletedBy(request.completedBy());
            }
            case abandoned -> {
                if (request.notes() == null || request.notes().isBlank()) {
                    throw new IllegalArgumentException("notes (reason) is required when abandoning a remediation");
                }
                remediation.setCompletedAt(now);
                remediation.setNotes(request.notes());
            }
            default -> {}
        }

        remediation.setStatus(targetStatus);
        remediationRepository.save(remediation);

        RemediationComment systemComment = RemediationComment.builder()
                .remediation(remediation)
                .author("system")
                .comment(String.format("Status changed from %s to %s", currentStatus, targetStatus))
                .build();
        remediationCommentRepository.save(systemComment);

        return RemediationMapper.toResponse(remediation);
    }

    public List<RemediationCommentResponse> getComments(Long remediationId) {
        return remediationCommentRepository.findByRemediationIdOrderByCreatedAtAsc(remediationId).stream()
                .map(RemediationCommentMapper::toResponse)
                .toList();
    }

    @Transactional
    public RemediationCommentResponse addComment(Long remediationId, AddRemediationCommentRequest request) {
        Remediation remediation = remediationRepository.getReferenceById(remediationId);
        RemediationComment comment = RemediationCommentMapper.toEntity(request, remediation);
        remediationCommentRepository.save(comment);
        return RemediationCommentMapper.toResponse(comment);
    }

    public RemediationStatisticsResponse getStatistics() {
        long total = remediationRepository.count();
        long planned = remediationRepository.countByStatus(RemediationStatus.planned);
        long inProgress = remediationRepository.countByStatus(RemediationStatus.in_progress);
        long completed = remediationRepository.countByStatus(RemediationStatus.completed);
        long abandoned = remediationRepository.countByStatus(RemediationStatus.abandoned);
        long overdue = remediationRepository.countOverdue(ACTIVE_STATUSES, LocalDate.now());

        Double completionRate = total > 0
                ? (completed * 100.0) / total
                : null;

        Double avgDays = remediationRepository.avgCompletionDays();

        return new RemediationStatisticsResponse(
                (int) total,
                (int) planned,
                (int) inProgress,
                (int) completed,
                (int) abandoned,
                (int) overdue,
                completionRate,
                avgDays,
                Instant.now()
        );
    }

    public List<RemediationResponse> getOverdueRemediations(RemediationPriority priority, String assignedTo) {
        return remediationRepository.findOverdueFiltered(
                        ACTIVE_STATUSES, LocalDate.now(), priority, assignedTo).stream()
                .map(RemediationMapper::toResponse)
                .toList();
    }

    private void validateTransition(RemediationStatus from, RemediationStatus to) {
        String target = to.name();
        boolean valid = switch (from) {
            case planned -> VALID_TRANSITIONS_FROM_PLANNED.contains(target);
            case in_progress -> VALID_TRANSITIONS_FROM_IN_PROGRESS.contains(target);
            case completed, abandoned -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", from, to));
        }
    }
}
