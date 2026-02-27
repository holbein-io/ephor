package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.scan.ScanResponse;
import io.holbein.ephor.api.service.ScansService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/scans")
@RequiredArgsConstructor
@Tag(name = "Scans", description = "Scan history")
public class ScansController {

    private final ScansService scansService;

    @GetMapping
    public List<ScanResponse> getAllScans(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return scansService.getAllScans(limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScanResponse> getScanById(@PathVariable Long id) {
        ScanResponse scan = scansService.findById(id);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scan);
    }
}
