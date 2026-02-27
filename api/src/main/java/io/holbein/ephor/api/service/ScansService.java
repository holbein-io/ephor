package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.scan.ScanResponse;
import io.holbein.ephor.api.mapper.scan.ScanMapper;
import io.holbein.ephor.api.repositories.ScanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScansService {

    private final ScanRepository scanRepository;

    public List<ScanResponse> getAllScans(int limit) {
        return scanRepository.findAllWithLimit(limit)
                .stream()
                .map(ScanMapper::toResponse)
                .toList();
    }

    public ScanResponse findById(long id) {
        return scanRepository.findById(id)
                .map(ScanMapper::toResponse)
                .orElse(null);
    }
}
