package io.holbein.ephor.api.enrichment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Pulls the two public exploitation feeds -- CISA KEV (JSON) and FIRST EPSS (gzip CSV) --
 * and stamps the matching {@code vulnerabilities} rows by cve_id (ADR-004). Both feeds are
 * global, so this belongs in the platform API, not the per-cluster scanner. Mirrors the
 * directory-sync pattern: WebClient in @PostConstruct, @Scheduled entry point, last-known-good
 * on failure (a failed feed logs and leaves prior values intact).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ephor.enrichment.enabled", havingValue = "true", matchIfMissing = true)
public class EnrichmentService {

    // EPSS uncompresses to ~250k rows; lift the WebClient's default 256KB buffer.
    private static final int MAX_FEED_BYTES = 64 * 1024 * 1024;

    private final EnrichmentProperties properties;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final PlatformTransactionManager transactionManager;

    private WebClient webClient;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void init() {
        // Both feeds 30x-redirect (EPSS hops host then to a dated filename daily), so follow them.
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_FEED_BYTES))
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
            KevCatalog catalog = webClient.get()
                    .uri(properties.getKevUrl())
                    .retrieve()
                    .bodyToMono(KevCatalog.class)
                    .block();

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
            byte[] gz = webClient.get()
                    .uri(properties.getEpssUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (gz == null || gz.length == 0) {
                log.warn("EPSS feed returned no data; keeping last-known-good");
                return;
            }

            // Only the CVEs we actually track; we never persist the ~250k rows we don't use.
            Set<String> tracked = new HashSet<>(vulnerabilityRepository.findDistinctCveIds());
            if (tracked.isEmpty()) {
                log.info("EPSS enrichment skipped: no tracked CVEs");
                return;
            }

            int matched = transactionTemplate.execute(tx -> applyEpss(gz, tracked));
            log.info("EPSS enrichment complete: {} of {} tracked CVEs scored", matched, tracked.size());
        } catch (Exception e) {
            log.error("EPSS enrichment failed; keeping last-known-good", e);
        }
    }

    private int applyEpss(byte[] gz, Set<String> tracked) {
        int matched = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ByteArrayInputStream(gz)), StandardCharsets.UTF_8))) {
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
                    vulnerabilityRepository.updateEpss(parts[0],
                            Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                    matched++;
                } catch (NumberFormatException ignored) {
                    // Malformed row; skip it rather than failing the whole feed.
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse EPSS CSV", e);
        }
        return matched;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KevCatalog(List<KevEntry> vulnerabilities) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KevEntry(@JsonProperty("cveID") String cveID,
                    @JsonProperty("dateAdded") String dateAdded) {}
}
