package io.holbein.ephor.api.service.triage;

import io.holbein.ephor.api.dto.triage.bulkplan.BulkPlanFilters;
import io.holbein.ephor.api.entity.TriagePreparation;
import io.holbein.ephor.api.entity.Vulnerability;

import java.util.List;

public final class BulkPlanFilterMatcher {

    private BulkPlanFilterMatcher() {}

    public static boolean matches(BulkPlanFilters filters, TriagePreparation prep) {
        if (filters == null) return true;

        Vulnerability vuln = prep.getVulnerability();

        if (!matchesList(filters.severity(), vuln.getSeverity())) return false;
        if (!matchesList(filters.packageName(), vuln.getPackageName())) return false;
        if (!matchesPattern(filters.packageNamePattern(), vuln.getPackageName())) return false;
        if (!matchesPattern(filters.cvePattern(), vuln.getCveId())) return false;
        if (!matchesList(filters.priorityFlag(), prep.getPriorityFlag())) return false;
        if (!matchesList(filters.prepStatus(), prep.getPrepStatus())) return false;

        return true;
    }

    private static <T> boolean matchesList(List<T> filterList, T value) {
        if (filterList == null || filterList.isEmpty()) return true;
        return filterList.contains(value);
    }

    private static boolean matchesPattern(String pattern, String value) {
        if (pattern == null || pattern.isBlank()) return true;
        if (value == null) return false;
        return value.matches(pattern);
    }
}
