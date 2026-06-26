package io.holbein.ephor.api.repositories;

public final class PriorityTierSql {

    private PriorityTierSql() {}

    public static final String TIER_CASE = """
            CASE
                WHEN NOT (COALESCE(BOOL_OR(vi.status IN ('open', 'triaged')), false)
                          AND (v.kev_listed OR COALESCE(v.epss_score, 0) >= :epssThreshold)) THEN 3
                WHEN v.fixed_version IS NOT NULL AND v.kev_listed THEN 0
                WHEN v.fixed_version IS NOT NULL THEN 1
                ELSE 2
            END""";
}
