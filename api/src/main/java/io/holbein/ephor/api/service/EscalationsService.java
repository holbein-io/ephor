package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.escalation.CreateEscalationRequest;
import io.holbein.ephor.api.dto.escalation.EscalationResponse;
import io.holbein.ephor.api.dto.escalation.UpdateEscalationRequest;
import io.holbein.ephor.api.entity.Escalation;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.mapper.escalation.EscalationMapper;
import io.holbein.ephor.api.repositories.EscalationRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationsService {

    private final EscalationRepository escalationRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    @Transactional(readOnly = true)
    public List<EscalationResponse> getAllEscalations() {
        return escalationRepository.findAll()
                .stream()
                .map(EscalationMapper::toResponse)
                .toList();
    }

    @Transactional
    public EscalationResponse createEscalation(CreateEscalationRequest request) {
        Vulnerability vulnerability = vulnerabilityRepository.getVulnerabilityById(request.vulnerabilityId());
        if (vulnerability == null) {
            throw ResourceNotFoundException.vulnerability(request.vulnerabilityId());
        }

        Escalation escalation = EscalationMapper.toEntity(request, vulnerability);
        Escalation saved = escalationRepository.save(escalation);
        log.info("Created escalation {} for vulnerability {}", saved.getId(), request.vulnerabilityId());
        return EscalationMapper.toResponse(saved);
    }

    @Transactional
    public EscalationResponse updateEscalation(Long id, UpdateEscalationRequest request) {
        Escalation escalation = escalationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.escalation(id));

        if (request.escalationLevel() != null) {
            escalation.setEscalationLevel(request.escalationLevel());
        }
        if (request.status() != null) {
            escalation.setStatus(request.status());
        }
        if (request.reason() != null) {
            escalation.setReason(request.reason());
        }
        if (request.msTeamsMessageId() != null) {
            escalation.setMsTeamsMessageId(request.msTeamsMessageId());
        }

        Escalation updated = escalationRepository.save(escalation);
        log.info("Updated escalation {}", id);
        return EscalationMapper.toResponse(updated);
    }
}
