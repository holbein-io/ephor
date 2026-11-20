package io.holbein.ephor.api.enrichment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Pulls the two public exploitation feeds -- CISA KEV (JSON) and FIRST EPSS (gzip CSV) --
 * and stamps the matching {@code vulnerabilities} rows by cve_id (ADR-004). Both feeds are
 * global, so this belongs in the platform API, not the per-cluster scanner. Mirrors the
 * directory-sync pattern: RestClient in @PostConstruct, @Scheduled entry point, last-known-good
 * on failure (a failed feed logs and leaves prior values intact).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ephor.enrichment.enabled", havingValue = "true", matchIfMissing = true)
public class EnrichmentService {

    private final EnrichmentProperties properties;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final PlatformTransactionManager transactionManager;
    private final ClientHttpRequestFactory outboundRequestFactory;

    private RestClient restClient;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void init() {
        this.restClient = RestClient.builder()
                .requestFactory(outboundRequestFactory)
                .build();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        log.info("Enrichment service initialized: kevUrl={}, epssUrl={}, epssThreshold={}",
                properties.getKevUrl(), properties.getEpssUrl(), properties.getEpssThreshold());
    }

    @Scheduled(fixedDelayString = "${ephor.enrichment.refresh-interval:86400000}",
            initialDelayString = "${ephor.enrichment.initial-delay:30000}")
    public void refresh() {
        refreshKev();
        refreshEpss();
    }

    /** CISA KEV catalog -> set kev_listed/kev_date_added; clear rows that dropped out. */
    void refreshKev() {
        try {
            KevCatalog catalog = restClient.get()
                    .uri(properties.getKevUrl())
                    .retrieve()
                    .body(KevCatalog.class);

            if (catalog == null || catalog.vulnerabilities() == null || catalog.vulnerabilities().isEmpty()) {
                log.warn("KEV feed returned no entries; keeping last-known-good");
                return;
            }

            transactionTemplate.executeWithoutResult(tx -> {
                Set<String> listed = new HashSet<>();
                for (KevEntry entry : catalog.vulnerabilities()) {
                    if (entry.cveID() == null || entry.cveID().isBlank()) {
                        continue;
                    }
                    vulnerabilityRepository.markKevListed(entry.cveID(), parseDate(entry.dateAdded()));
                    listed.add(entry.cveID());
                }
                // Guard against NOT IN (): only reachable if every entry had a blank id.
                if (!listed.isEmpty()) {
                    vulnerabilityRepository.clearKevExcept(listed);
                }
            });

            log.info("KEV enrichment complete: {} catalog entries", catalog.vulnerabilities().size());
        } catch (Exception e) {
            log.error("KEV enrichment failed; keeping last-known-good", e);
        }
    }

    /** FIRST EPSS daily CSV -> set epss_score/epss_percentile for tracked CVEs only. */
    void refreshEpss() {
        try {
            // Only the CVEs we actually track; we never hold the ~250k rows we don't use.
            Set<String> tracked = new HashSet<>(vulnerabilityRepository.findDistinctCveIds());
            if (tracked.isEmpty()) {
                log.info("EPSS enrichment skipped: no tracked CVEs");
                return;
            }

            // Parsed while streaming, outside the transaction: the download must not hold a DB connection.
            Map<String, EpssScore> scores = restClient.get()
                    .uri(properties.getEpssUrl())
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw new IllegalStateException("EPSS feed returned " + response.getStatusCode());
                        }
                        return parseEpss(response.getBody(), tracked);
                    });

            if (scores == null || scores.isEmpty()) {
                log.warn("EPSS feed matched no tracked CVEs; keeping last-known-good");
                return;
            }

            transactionTemplate.executeWithoutResult(tx -> scores.forEach((cveId, score) ->
                    vulnerabilityRepository.updateEpss(cveId, score.score(), score.percentile())));
            log.info("EPSS enrichment complete: {} of {} tracked CVEs scored", scores.size(), tracked.size());
        } catch (Exception e) {
            log.error("EPSS enrichment failed; keeping last-known-good", e);
        }
    }

    Map<String, EpssScore> parseEpss(InputStream gz, Set<String> tracked) {
        Map<String, EpssScore> scores = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(gz), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip the leading "#model_version,..." comment and the "cve,epss,percentile" header.
                if (line.isBlank() || line.startsWith("#") || line.startsWith("cve,")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 3 || !tracked.contains(parts[0])) {
                    continue;
                }
                try {
                    scores.put(parts[0], new EpssScore(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                } catch (NumberFormatException ignored) {
                    // Malformed row; skip it rather than failing the whole feed.
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse EPSS CSV", e);
        }
        return scores;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    record EpssScore(double score, double percentile) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KevCatalog(List<KevEntry> vulnerabilities) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KevEntry(@JsonProperty("cveID") String cveID,
                    @JsonProperty("dateAdded") String dateAdded) {}
}
