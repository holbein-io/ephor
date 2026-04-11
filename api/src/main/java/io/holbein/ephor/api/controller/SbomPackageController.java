package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.sbom.LicenseDistributionEntry;
import io.holbein.ephor.api.dto.sbom.PackageSearchResult;
import io.holbein.ephor.api.dto.sbom.TopPackageEntry;
import io.holbein.ephor.api.service.SbomQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sbom/packages")
@RequiredArgsConstructor
@Tag(name = "SBOM Packages", description = "Cross-fleet package search")
public class SbomPackageController {

    private final SbomQueryService sbomQueryService;

    @GetMapping("/search")
    public Page<PackageSearchResult> search(
            @RequestParam("name") String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        return sbomQueryService.searchPackages(name, type, PageRequest.of(page, size));
    }

    @GetMapping("/images")
    public ResponseEntity<List<String>> imagesByPackage(
            @RequestParam("name") String name,
            @RequestParam(value = "version", required = false) String version) {
        return ResponseEntity.ok(sbomQueryService.findImagesByPackage(name, version));
    }

    @GetMapping("/top")
    public Page<TopPackageEntry> topPackages(
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return sbomQueryService.getTopPackages(PageRequest.of(0, size));
    }

    @GetMapping("/licenses")
    public List<LicenseDistributionEntry> licenseDistribution() {
        return sbomQueryService.getLicenseDistribution();
    }

    @GetMapping("/licenses/search")
    public Page<PackageSearchResult> searchByLicense(
            @RequestParam("license") String license,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        return sbomQueryService.findByLicense(license, PageRequest.of(page, size));
    }
}
