package io.holbein.ephor.api.unit;

import io.holbein.ephor.api.dto.triage.bulkplan.BulkPlanFilters;
import io.holbein.ephor.api.entity.TriagePreparation;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.model.enums.PrepStatus;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.service.triage.BulkPlanFilterMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BulkPlanFilterMatcherTest {

    @Test
    void nullFilters_matchesEverything() {
        assertTrue(BulkPlanFilterMatcher.matches(null, prep(SeverityLevel.HIGH, "openssl", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void emptyFilters_matchesEverything() {
        var filters = new BulkPlanFilters(null, null, null, null, null, null, null);
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.LOW, "curl", PriorityFlag.low, PrepStatus.reviewed)));
    }

    @Test
    void severityFilter_matches() {
        var filters = new BulkPlanFilters(List.of(SeverityLevel.CRITICAL, SeverityLevel.HIGH), null, null, null, null, null, null);
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "pkg", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void severityFilter_noMatch() {
        var filters = new BulkPlanFilters(List.of(SeverityLevel.CRITICAL), null, null, null, null, null, null);
        assertFalse(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.LOW, "pkg", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void packageNameFilter_matches() {
        var filters = new BulkPlanFilters(null, null, List.of("openssl", "curl"), null, null, null, null);
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "openssl", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void packageNameFilter_noMatch() {
        var filters = new BulkPlanFilters(null, null, List.of("openssl"), null, null, null, null);
        assertFalse(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "curl", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void packageNamePattern_matchesRegex() {
        var filters = new BulkPlanFilters(null, null, null, "^linux-.*", null, null, null);
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "linux-kernel", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void packageNamePattern_noMatch() {
        var filters = new BulkPlanFilters(null, null, null, "^linux-.*", null, null, null);
        assertFalse(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "openssl", PriorityFlag.medium, PrepStatus.pending)));
    }

    @Test
    void cvePattern_matchesRegex() {
        var filters = new BulkPlanFilters(null, null, null, null, "CVE-2025-.*", null, null);
        var p = prep(SeverityLevel.HIGH, "pkg", PriorityFlag.medium, PrepStatus.pending);
        p.getVulnerability().setCveId("CVE-2025-1234");
        assertTrue(BulkPlanFilterMatcher.matches(filters, p));
    }

    @Test
    void cvePattern_noMatch() {
        var filters = new BulkPlanFilters(null, null, null, null, "CVE-2025-.*", null, null);
        var p = prep(SeverityLevel.HIGH, "pkg", PriorityFlag.medium, PrepStatus.pending);
        p.getVulnerability().setCveId("CVE-2024-5678");
        assertFalse(BulkPlanFilterMatcher.matches(filters, p));
    }

    @Test
    void priorityFlagFilter_matches() {
        var filters = new BulkPlanFilters(null, null, null, null, null, List.of(PriorityFlag.high, PriorityFlag.critical), null);
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "pkg", PriorityFlag.high, PrepStatus.pending)));
    }

    @Test
    void priorityFlagFilter_noMatch() {
        var filters = new BulkPlanFilters(null, null, null, null, null, List.of(PriorityFlag.critical), null);
        assertFalse(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "pkg", PriorityFlag.low, PrepStatus.pending)));
    }

    @Test
    void prepStatusFilter_matches() {
        var filters = new BulkPlanFilters(null, null, null, null, null, null, List.of(PrepStatus.reviewed, PrepStatus.flagged));
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "pkg", PriorityFlag.medium, PrepStatus.reviewed)));
    }

    @Test
    void allFilters_andLogic() {
        var filters = new BulkPlanFilters(
                List.of(SeverityLevel.HIGH),
                null,
                List.of("openssl"),
                null,
                null,
                List.of(PriorityFlag.high),
                List.of(PrepStatus.reviewed)
        );
        // All match
        assertTrue(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "openssl", PriorityFlag.high, PrepStatus.reviewed)));
        // Severity doesn't match
        assertFalse(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.LOW, "openssl", PriorityFlag.high, PrepStatus.reviewed)));
        // Package doesn't match
        assertFalse(BulkPlanFilterMatcher.matches(filters, prep(SeverityLevel.HIGH, "curl", PriorityFlag.high, PrepStatus.reviewed)));
    }

    @Test
    void patternFilter_nullValue_noMatch() {
        var filters = new BulkPlanFilters(null, null, null, "^linux-.*", null, null, null);
        var p = prep(SeverityLevel.HIGH, null, PriorityFlag.medium, PrepStatus.pending);
        assertFalse(BulkPlanFilterMatcher.matches(filters, p));
    }

    private TriagePreparation prep(SeverityLevel severity, String packageName, PriorityFlag flag, PrepStatus status) {
        Vulnerability vuln = Vulnerability.builder()
                .cveId("CVE-2025-0001")
                .packageName(packageName)
                .packageVersion("1.0")
                .severity(severity)
                .scannerType("trivy")
                .build();
        return TriagePreparation.builder()
                .vulnerability(vuln)
                .priorityFlag(flag)
                .prepStatus(status)
                .prepBy("tester")
                .build();
    }
}
